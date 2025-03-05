package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.Disk;
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

  // This is temporary while we wait for Leonardo autopause to rollout. Once
  // available, we should instead take a runtime status of STOPPED to trigger
  // idle deletion.
  private static final int IDLE_AFTER_HOURS = 3;

  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  private final LeonardoApiClient leonardoApiClient;
  private final LeonardoMapper leonardoMapper;
  private final TaskQueueService taskQueueService;
  private final WorkspaceService workspaceService;

  @Autowired
  OfflineEnvironmentsController(
      Clock clock,
      LeonardoApiClient leonardoApiClient,
      LeonardoMapper leonardoMapper,
      Provider<WorkbenchConfig> configProvider,
      TaskQueueService taskQueueService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.configProvider = configProvider;
    this.leonardoApiClient = leonardoApiClient;
    this.leonardoMapper = leonardoMapper;
    this.taskQueueService = taskQueueService;
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
    final List<Disk> disks =
        leonardoApiClient.listDisksAsService().stream().map(leonardoMapper::toApiDisk).toList();
    log.info(String.format("Queueing %d persistent disks for idleness check.", disks.size()));
    taskQueueService.groupAndPushCheckPersistentDiskTasks(disks);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteUnsharedWorkspaceEnvironments() {
    List<String> activeNamespaces =
        workspaceService.listWorkspacesAsService().stream()
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
