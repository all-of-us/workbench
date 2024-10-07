package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoDiskStatus;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.utils.BillingUtils;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline notebook runtime API. Handles cronjobs for cleanup and upgrade of older runtimes. Methods
 * here should be access restricted as, unlike RuntimeController, these endpoints run as the
 * Workbench service account.
 */
@RestController
public class OfflineRuntimeController implements OfflineRuntimeApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineRuntimeController.class.getName());

  // On the n'th day of inactivity on a PD, a notification is sent.
  private static final Set<Integer> INACTIVE_DISK_NOTIFY_THRESHOLDS_DAYS = ImmutableSet.of(14);
  // Every n'th day of inactivity on a PD (not including 0), a notification is sent.
  private static final int INACTIVE_DISK_NOTIFY_PERIOD_DAYS = 30;

  // This is temporary while we wait for Leonardo autopause to rollout. Once
  // available, we should instead take a runtime status of STOPPED to trigger
  // idle deletion.
  private static final int IDLE_AFTER_HOURS = 3;

  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final MailService mailService;
  private final LeonardoApiClient leonardoApiClient;
  private final Provider<WorkbenchConfig> configProvider;
  private final WorkspaceDao workspaceDao;
  private final UserDao userDao;
  private final LeonardoMapper leonardoMapper;
  private final Clock clock;

  @Autowired
  OfflineRuntimeController(
      FireCloudService firecloudService,
      FreeTierBillingService freeTierBillingService,
      MailService mailService,
      LeonardoApiClient leonardoApiClient,
      Provider<WorkbenchConfig> configProvider,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      Clock clock,
      LeonardoMapper leonardoMapper) {
    this.fireCloudService = firecloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.mailService = mailService;
    this.leonardoApiClient = leonardoApiClient;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
    this.clock = clock;
    this.configProvider = configProvider;
    this.leonardoMapper = leonardoMapper;
  }

  /**
   * deleteOldRuntimes deletes older runtimes in order to force an upgrade on the next researcher
   * login. This method is meant to be restricted to invocation by App Engine cron.
   *
   * <p>The runtime deletion policy here aims to strike a balance between enforcing upgrades, cost
   * savings, and minimizing user disruption. To this point, our goal is to only upgrade idle
   * runtimes if possible, as doing so gives us an assurance that a researcher is not actively using
   * it. We delete runtimes in the following cases:
   *
   * <ol>
   *   <li>It exceeds the max runtime age. Per environment, but O(weeks).
   *   <li>It is idle and exceeds the max idle runtime age. Per environment, smaller than (1).
   * </ol>
   *
   * <p>As an App Engine cron endpoint, the runtime of this method may not exceed 10 minutes.
   */
  @Override
  public ResponseEntity<Void> deleteOldRuntimes() {
    final Instant now = clock.instant();
    final WorkbenchConfig config = configProvider.get();
    final Duration maxAge = Duration.ofDays(config.firecloud.notebookRuntimeMaxAgeDays);
    final Duration idleMaxAge = Duration.ofDays(config.firecloud.notebookRuntimeIdleMaxAgeDays);

    final List<LeonardoListRuntimeResponse> listRuntimeResponses =
        leonardoApiClient.listRuntimesAsService();

    int idles = 0;
    int activeDeletes = 0;
    int unusedDeletes = 0;
    for (LeonardoListRuntimeResponse listRuntimeResponse : listRuntimeResponses) {
      final String googleProject =
          leonardoMapper.toGoogleProject(listRuntimeResponse.getCloudContext());
      final String runtimeId =
          String.format("%s/%s", googleProject, listRuntimeResponse.getRuntimeName());

      // Refetch the runtime to ensure freshness,
      // as this iteration may take some time.
      final LeonardoGetRuntimeResponse runtime =
          leonardoApiClient.getRuntimeAsService(
              googleProject, listRuntimeResponse.getRuntimeName());

      if (LeonardoRuntimeStatus.UNKNOWN.equals(runtime.getStatus())
          || runtime.getStatus() == null) {
        log.warning(String.format("unknown runtime status for runtime '%s'", runtimeId));
        continue;
      }
      if (!LeonardoRuntimeStatus.RUNNING.equals(runtime.getStatus())
          && !LeonardoRuntimeStatus.STOPPED.equals(runtime.getStatus())) {
        // For now, we only handle running or stopped (suspended) runtimes.
        continue;
      }

      final Instant lastUsed = Instant.parse(runtime.getAuditInfo().getDateAccessed());
      final boolean isIdle = Duration.between(lastUsed, now).toHours() > IDLE_AFTER_HOURS;
      if (isIdle) {
        idles++;
      }

      final Instant created = Instant.parse(runtime.getAuditInfo().getCreatedDate());
      final Duration age = Duration.between(created, now);
      if (age.toMillis() > maxAge.toMillis()) {
        log.info(
            String.format(
                "deleting runtime '%s', exceeded max lifetime @ %s (>%s). latest accessed time: %s",
                runtimeId,
                formatDuration(age),
                formatDuration(maxAge),
                runtime.getAuditInfo().getDateAccessed()));
        activeDeletes++;
      } else if (isIdle && age.toMillis() > idleMaxAge.toMillis()) {
        log.info(
            String.format(
                "deleting runtime '%s', idle with age %s (>%s)",
                runtimeId, formatDuration(age), formatDuration(idleMaxAge)));
        unusedDeletes++;
      } else {
        // Don't delete.
        continue;
      }

      leonardoApiClient.deleteRuntimeAsService(googleProject, runtime.getRuntimeName());
    }
    log.info(
        String.format(
            "deleted %d old runtimes and %d idle runtimes "
                + "of %d total runtimes (%d of which were idle)",
            activeDeletes, unusedDeletes, listRuntimeResponses.size(), idles));

    return ResponseEntity.noContent().build();
  }

  private static String formatDuration(Duration d) {
    if ((d.toHours() % 24) == 0) {
      return String.format("%dd", d.toDays());
    }
    return String.format("%dd %dh", d.toDays(), d.toHours() % 24);
  }

  @Override
  public ResponseEntity<Void> checkPersistentDisks() {
    // Fetch disks as the service, which gets all disks for all workspaces.
    final List<LeonardoListPersistentDiskResponse> disks = leonardoApiClient.listDisksAsService();

    // Bucket disks by days since last access.
    final Instant now = clock.instant();
    Map<Integer, List<LeonardoListPersistentDiskResponse>> disksByDaysUnused =
        disks.stream()
            .filter(disk -> LeonardoDiskStatus.READY.equals(disk.getStatus()))
            .collect(
                Collectors.groupingBy(
                    disk -> {
                      Instant lastAccessed = Instant.parse(disk.getAuditInfo().getDateAccessed());
                      return (int) Duration.between(lastAccessed, now).toDays();
                    }));

    // Dispatch notifications if any disks are the right number of days old.
    int notifySuccess = 0;
    int notifySkip = 0;
    int notifyFail = 0;
    Exception lastException = null;
    for (int daysUnused : disksByDaysUnused.keySet()) {
      if (daysUnused <= 0) {
        // Our periodic notifications should not trigger on day 0.
        continue;
      }

      if (!INACTIVE_DISK_NOTIFY_THRESHOLDS_DAYS.contains(daysUnused)
          && daysUnused % INACTIVE_DISK_NOTIFY_PERIOD_DAYS != 0) {
        continue;
      }

      for (LeonardoListPersistentDiskResponse disk : disksByDaysUnused.get(daysUnused)) {
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
                  "failed to send notification for disk '%s/%s'",
                  leonardoMapper.toGoogleProject(disk.getCloudContext()), disk.getName()),
              e);
          lastException = e;
          notifyFail++;
        }
      }
    }

    log.info(
        String.format(
            "sent %d notifications successfully (%d skipped, %d failed)",
            notifySuccess, notifySkip, notifyFail));
    if (lastException != null) {
      throw new ServerErrorException(
          String.format(
              "%d/%d disk notifications failed to send, see logs for details",
              notifyFail, notifySuccess + notifyFail + notifySkip),
          lastException);
    }

    return ResponseEntity.noContent().build();
  }

  // Returns true if an email is sent.
  private boolean notifyForUnusedDisk(LeonardoListPersistentDiskResponse disk, int daysUnused)
      throws MessagingException {
    String googleProject = leonardoMapper.toGoogleProject(disk.getCloudContext());
    final boolean attached;
    try {
      attached = isDiskAttached(disk, googleProject);
    } catch (ApiException | NullPointerException ex) {
      log.warning(
          String.format(
              "skipping disk '%s' error while getting disk status", disk.getName(), googleProject));
      return false;
    }

    Optional<DbWorkspace> workspace = workspaceDao.getByGoogleProject(googleProject);
    if (workspace.isEmpty()) {
      log.warning(
          String.format(
              "skipping disk '%s' associated with unknown Google project '%s'",
              disk.getName(), googleProject));
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
                        || entry.getKey().equals(disk.getAuditInfo().getCreator()))
            .map(Entry::getKey)
            .collect(Collectors.toList());

    // Lookup these users in the database, so we can get their contact email, etc.
    List<DbUser> dbUsers = userDao.findUserByUsernameIn(notifyUsernames);
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
          freeTierBillingService.getWorkspaceCreatorFreeCreditsRemaining(workspace.get());
    }

    mailService.alertUsersUnusedDiskWarningThreshold(
        dbUsers, workspace.get(), disk, attached, daysUnused, initialCreditsRemaining);
    return true;
  }

  private boolean isDiskAttached(
      LeonardoListPersistentDiskResponse diskResponse, String googleProject) throws ApiException {
    final String diskName = diskResponse.getName();

    if (leonardoMapper.toApiListDisksResponse(diskResponse).isGceRuntime()) {
      return leonardoApiClient.listRuntimesByProjectAsService(googleProject).stream()
          .flatMap(runtime -> Stream.ofNullable(runtime.getDiskConfig()))
          .anyMatch(diskConfig -> diskName.equals(diskConfig.getName()));
    } else {
      return leonardoApiClient.listAppsInProjectAsService(googleProject).stream()
          .anyMatch(userAppEnvironment -> diskName.equals(userAppEnvironment.getDiskName()));
    }
  }
}
