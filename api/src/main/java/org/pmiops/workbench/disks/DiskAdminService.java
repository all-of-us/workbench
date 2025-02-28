package org.pmiops.workbench.disks;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfigInResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.utils.BillingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiskAdminService {
  private static final Logger log = Logger.getLogger(DiskAdminService.class.getName());

  // On the 14th day of inactivity on a PD, a notification is sent.
  private static final Set<Integer> INACTIVE_DISK_NOTIFY_THRESHOLDS_DAYS = ImmutableSet.of(14);
  // Every 30th day of inactivity on a PD (not including 0), a notification is sent.
  private static final int INACTIVE_DISK_NOTIFY_PERIOD_DAYS = 30;

  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final InitialCreditsService initialCreditsService;
  private final LeonardoApiClient leonardoApiClient;
  private final MailService mailService;
  private final Provider<WorkbenchConfig> configProvider;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public DiskAdminService(
      Clock clock,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      LeonardoApiClient leonardoApiClient,
      MailService mailService,
      Provider<WorkbenchConfig> configProvider,
      UserDao userDao,
      WorkspaceDao workspaceDao) {
    this.clock = clock;
    this.configProvider = configProvider;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
    this.leonardoApiClient = leonardoApiClient;
    this.mailService = mailService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
  }

  public void checkPersistentDisks(List<Disk> disks) {
    log.info(String.format("Checking %d persistent disks for idleness...", disks.size()));

    // Bucket disks by days since last access.
    final Instant now = clock.instant();
    Map<Integer, List<Disk>> disksByDaysUnused =
        disks.stream()
            .filter(disk -> DiskStatus.READY.equals(disk.getStatus()))
            .collect(
                Collectors.groupingBy(
                    disk -> {
                      Instant lastAccessed = Instant.parse(disk.getDateAccessed());
                      return (int) Duration.between(lastAccessed, now).toDays();
                    },
                    TreeMap::new, // sorts the resulting map by the key (days since last access)
                    Collectors.toList()));

    // Dispatch notifications if any disks are the right number of days old.
    int notifySuccess = 0;
    int notifySkip = 0;
    int notifyFail = 0;
    boolean sawException = false;
    for (var entry : disksByDaysUnused.entrySet()) {
      int daysUnused = entry.getKey();
      List<Disk> disksForDay = entry.getValue();

      boolean wantToNotify = shouldNotify(daysUnused);
      String sendOrNot = wantToNotify ? "sending" : "not sending";
      log.info(
          String.format(
              "checkPersistentDisks: %s notifications for %d disks which have been idle for %d days",
              sendOrNot, disksForDay.size(), daysUnused));

      if (!wantToNotify) {
        continue;
      }

      for (Disk disk : disksForDay) {
        try {
          if (notifyForUnusedDisk(disk, daysUnused)) {
            notifySuccess++;
          } else {
            notifySkip++;
          }
        } catch (MessagingException e) {
          log.log(
              Level.WARNING,
              String.format(
                  "checkPersistentDisks: failed to send notification for disk '%s/%s'",
                  disk.getGoogleProject(), disk.getName()),
              e);
          sawException = true;
          notifyFail++;
        }
      }
    }

    log.info(
        String.format(
            "checkPersistentDisks: sent %d notifications successfully (%d skipped, %d failed)",
            notifySuccess, notifySkip, notifyFail));
    if (sawException) {
      log.warning(
          String.format(
              "checkPersistentDisks: %d/%d disk notifications failed to send, see logs for details",
              notifyFail, notifySuccess + notifyFail + notifySkip));
    }
  }

  private boolean shouldNotify(int daysUnused) {
    return daysUnused > 0
        && (INACTIVE_DISK_NOTIFY_THRESHOLDS_DAYS.contains(daysUnused)
            || daysUnused % INACTIVE_DISK_NOTIFY_PERIOD_DAYS == 0);
  }

  // Returns true if an email is sent.
  private boolean notifyForUnusedDisk(Disk disk, int daysUnused) throws MessagingException {
    final boolean attached;
    try {
      attached = isDiskAttached(disk);
    } catch (ApiException | NullPointerException ex) {
      log.warning(
          String.format(
              "skipping disk '%s/%s' error while getting disk status",
              disk.getGoogleProject(), disk.getName()));
      return false;
    }

    Optional<DbWorkspace> workspace = workspaceDao.getByGoogleProject(disk.getGoogleProject());
    if (workspace.isEmpty()) {
      log.warning(
          String.format(
              "skipping disk '%s' associated with unknown Google project '%s'",
              disk.getName(), disk.getGoogleProject()));
      return false;
    }

    // Lookup the owners and disk creators.
    RawlsWorkspaceACL acl =
        fireCloudService.getWorkspaceAclAsService(
            workspace.get().getWorkspaceNamespace(), workspace.get().getFirecloudName());
    Map<String, RawlsWorkspaceAccessEntry> aclMap = FirecloudTransforms.extractAclResponse(acl);
    List<String> notifyUsernames =
        aclMap.entrySet().stream()
            .filter(
                entry ->
                    WorkspaceAccessLevel.OWNER.toString().equals(entry.getValue().getAccessLevel())
                        || entry.getKey().equals(disk.getCreator()))
            .map(Entry::getKey)
            .toList();

    // Lookup these users in the database, so we can get their contact email, etc.
    List<DbUser> dbUsers = userDao.findUsersByUsernameIn(notifyUsernames);
    if (dbUsers.size() != notifyUsernames.size()) {
      log.warning(
          "failed to lookup one or more users in the database: "
              + Sets.difference(
                      dbUsers.stream().map(DbUser::getUsername).collect(Collectors.toSet()),
                      new HashSet<>(notifyUsernames))
                  .stream()
                  .collect(Collectors.joining(",")));
    }
    if (dbUsers.isEmpty()) {
      log.warning("found no users to notify");
      return false;
    }

    Double initialCreditsRemaining = null;
    if (BillingUtils.isInitialCredits(
        workspace.get().getBillingAccountName(), configProvider.get())) {
      initialCreditsRemaining =
          initialCreditsService.getWorkspaceCreatorFreeCreditsRemaining(workspace.get());
    }

    mailService.alertUsersUnusedDiskWarningThreshold(
        dbUsers, workspace.get(), disk, attached, daysUnused, initialCreditsRemaining);
    return true;
  }

  @VisibleForTesting
  public boolean isDiskAttached(Disk disk) throws ApiException {
    final String diskName = disk.getName();

    if (disk.isGceRuntime()) {
      return leonardoApiClient.listRuntimesByProjectAsService(disk.getGoogleProject()).stream()
          // this filter/map follows the discriminator logic in the source Leonardo Swagger
          // for OneOfRuntimeConfigInResponse
          .filter(
              resp -> {
                var runtimeConfig =
                    new Gson()
                        .fromJson(
                            new Gson().toJson(resp.getRuntimeConfig()),
                            LeonardoRuntimeConfig.class);
                return CloudServiceEnum.GCE.equals(runtimeConfig.getCloudService());
              })
          .map(
              resp ->
                  new Gson()
                      .fromJson(
                          new Gson().toJson(resp.getRuntimeConfig()),
                          LeonardoGceWithPdConfigInResponse.class))
          .anyMatch(
              gceWithPdConfig ->
                  disk.getPersistentDiskId().equals(gceWithPdConfig.getPersistentDiskId()));
    } else {
      return leonardoApiClient.listAppsInProjectAsService(disk.getGoogleProject()).stream()
          .anyMatch(userAppEnvironment -> diskName.equals(userAppEnvironment.getDiskName()));
    }
  }
}
