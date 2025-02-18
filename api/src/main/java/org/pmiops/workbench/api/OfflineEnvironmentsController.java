package org.pmiops.workbench.api;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.legacy_leonardo_client.ApiException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGceWithPdConfigInResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.utils.BillingUtils;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline notebook runtime API. Handles cronjobs for cleanup and upgrade of older runtimes. Methods
 * here should be access restricted as, unlike RuntimeController, these endpoints run as the
 * Workbench service account.
 */
@RestController
public class OfflineEnvironmentsController implements OfflineEnvironmentsApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineEnvironmentsController.class.getName());

  // On the n'th day of inactivity on a PD, a notification is sent.
  private static final Set<Integer> INACTIVE_DISK_NOTIFY_THRESHOLDS_DAYS = ImmutableSet.of(14);
  // Every n'th day of inactivity on a PD (not including 0), a notification is sent.
  private static final int INACTIVE_DISK_NOTIFY_PERIOD_DAYS = 30;

  // This is temporary while we wait for Leonardo autopause to rollout. Once
  // available, we should instead take a runtime status of STOPPED to trigger
  // idle deletion.
  private static final int IDLE_AFTER_HOURS = 3;

  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  private final FireCloudService fireCloudService;
  private final InitialCreditsService initialCreditsService;
  private final LeonardoApiClient leonardoApiClient;
  private final LeonardoMapper leonardoMapper;
  private final MailService mailService;
  private final TaskQueueService taskQueueService;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceService workspaceService;

  @Autowired
  OfflineEnvironmentsController(
      Clock clock,
      FireCloudService firecloudService,
      InitialCreditsService initialCreditsService,
      LeonardoApiClient leonardoApiClient,
      LeonardoMapper leonardoMapper,
      MailService mailService,
      Provider<WorkbenchConfig> configProvider,
      TaskQueueService taskQueueService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.configProvider = configProvider;
    this.fireCloudService = firecloudService;
    this.initialCreditsService = initialCreditsService;
    this.leonardoApiClient = leonardoApiClient;
    this.leonardoMapper = leonardoMapper;
    this.mailService = mailService;
    this.taskQueueService = taskQueueService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceService = workspaceService;
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

    final List<LeonardoListRuntimeResponse> responses = leonardoApiClient.listRuntimesAsService();

    log.info(String.format("Checking %d runtimes for age and idleness...", responses.size()));

    int idles = 0;
    int activeDeletes = 0;
    int unusedDeletes = 0;
    for (LeonardoListRuntimeResponse listRuntimeResponse : responses) {
      final String googleProject =
          leonardoMapper.toGoogleProject(listRuntimeResponse.getCloudContext());
      final String runtimeId =
          String.format("%s/%s", googleProject, listRuntimeResponse.getRuntimeName());

      // Refetch the runtime to ensure freshness,
      // as this iteration may take some time.
      final LeonardoGetRuntimeResponse runtime;
      try {
        runtime =
            leonardoApiClient.getRuntimeAsService(
                googleProject, listRuntimeResponse.getRuntimeName());
      } catch (WorkbenchException e) {
        log.warning(String.format("error refetching runtime '%s': %s", runtimeId, e.getMessage()));
        continue;
      }

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

      leonardoApiClient.deleteRuntimeAsService(
          googleProject, runtime.getRuntimeName(), /* deleteDisk */ false);
    }
    log.info(
        String.format(
            "Deleted %d old runtimes and %d idle runtimes "
                + "of %d total runtimes (%d of which were idle)",
            activeDeletes, unusedDeletes, responses.size(), idles));

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
    final List<ListPersistentDiskResponse> disks = leonardoApiClient.listDisksAsService();

    log.info(String.format("Checking %d persistent disks for idleness...", disks.size()));

    // Bucket disks by days since last access.
    final Instant now = clock.instant();
    Map<Integer, List<ListPersistentDiskResponse>> disksByDaysUnused =
        disks.stream()
            .filter(disk -> DiskStatus.READY.equals(disk.getStatus()))
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

      for (ListPersistentDiskResponse disk : disksByDaysUnused.get(daysUnused)) {
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
                  leonardoMapper.toGoogleProject(disk.getCloudContext()), disk.getName()),
              e);
          lastException = e;
          notifyFail++;
        }
      }
    }

    log.info(
        String.format(
            "checkPersistentDisks: sent %d notifications successfully (%d skipped, %d failed)",
            notifySuccess, notifySkip, notifyFail));
    if (lastException != null) {
      throw new ServerErrorException(
          String.format(
              "checkPersistentDisks: %d/%d disk notifications failed to send, see logs for details",
              notifyFail, notifySuccess + notifyFail + notifySkip),
          lastException);
    }

    return ResponseEntity.noContent().build();
  }

  // Returns true if an email is sent.
  private boolean notifyForUnusedDisk(ListPersistentDiskResponse disk, int daysUnused)
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
  public boolean isDiskAttached(ListPersistentDiskResponse diskResponse, String googleProject)
      throws ApiException {
    final String diskName = diskResponse.getName();

    if (leonardoMapper.toApiListDisksResponse(diskResponse).isGceRuntime()) {
      return leonardoApiClient.listRuntimesByProjectAsService(googleProject).stream()
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
                  diskResponse.getId().equals(gceWithPdConfig.getPersistentDiskId()));
    } else {
      return leonardoApiClient.listAppsInProjectAsService(googleProject).stream()
          .anyMatch(userAppEnvironment -> diskName.equals(userAppEnvironment.getDiskName()));
    }
  }

  @Override
  public ResponseEntity<Void> deleteUnsharedWorkspaceEnvironments() {
    List<String> activeNamespaces =
        workspaceService.getWorkspacesAsService().stream()
            .map(ws -> ws.getWorkspace().getNamespace())
            .toList();
    log.info(
        String.format(
            "Queuing %d active workspaces in batches for deletion of unshared resources",
            activeNamespaces.size()));
    taskQueueService.groupAndPushDeleteWorkspaceEnvironmentTasks(activeNamespaces);
    return ResponseEntity.noContent().build();
  }
}
