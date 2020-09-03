package org.pmiops.workbench.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline cluster API. Handles cronjobs for cleanup and upgrade of older runtimes. Methods here
 * should be access restricted as, unlike RuntimeController, these endpoints run as the Workbench
 * service account.
 */
@RestController
public class OfflineRuntimeController implements OfflineRuntimeApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineRuntimeController.class.getName());

  // This is temporary while we wait for Leonardo autopause to rollout. Once
  // available, we should instead take a cluster status of STOPPED to trigger
  // idle deletion.
  private static final int IDLE_AFTER_HOURS = 3;

  private final Provider<RuntimesApi> runtimesApiProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  @Autowired
  OfflineRuntimeController(
      @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API) Provider<RuntimesApi> runtimesApiProvider,
      Provider<WorkbenchConfig> configProvider,
      Clock clock) {
    this.clock = clock;
    this.configProvider = configProvider;
    this.runtimesApiProvider = runtimesApiProvider;
  }

  /**
   * checkRuntimes deletes older runtimes in order to force an upgrade on the next researcher login.
   * This method is meant to be restricted to invocation by App Engine cron.
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
  public ResponseEntity<Void> checkRuntimes() {
    final Instant now = clock.instant();
    final WorkbenchConfig config = configProvider.get();
    final Duration maxAge = Duration.ofDays(config.firecloud.notebookRuntimeMaxAgeDays);
    final Duration idleMaxAge = Duration.ofDays(config.firecloud.notebookRuntimeIdleMaxAgeDays);

    final RuntimesApi runtimesApi = runtimesApiProvider.get();
    final List<LeonardoListRuntimeResponse> listRuntimeResponses;
    try {
      listRuntimeResponses = runtimesApi.listRuntimes(null, false);
    } catch (ApiException e) {
      throw ExceptionUtils.convertLeonardoException(e);
    }

    int errors = 0;
    int idles = 0;
    int activeDeletes = 0;
    int unusedDeletes = 0;
    for (LeonardoListRuntimeResponse listRuntimeResponse : listRuntimeResponses) {
      final String runtimeId =
          listRuntimeResponse.getGoogleProject() + "/" + listRuntimeResponse.getRuntimeName();
      final LeonardoGetRuntimeResponse runtime;
      try {
        // Refetch the runtime to ensure freshness as this iteration may take
        // some time.
        runtime =
            runtimesApi.getRuntime(
                listRuntimeResponse.getGoogleProject(), listRuntimeResponse.getRuntimeName());
      } catch (ApiException e) {
        log.log(Level.WARNING, String.format("failed to refetch runtime '%s'", runtimeId), e);
        errors++;
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
                "deleting runtime '%s', exceeded max lifetime @ %s (>%s)",
                runtimeId, formatDuration(age), formatDuration(maxAge)));
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
      try {
        runtimesApi.deleteRuntime(
            runtime.getGoogleProject(), runtime.getRuntimeName(), /* includeDisk */ false);
      } catch (ApiException e) {
        log.log(Level.WARNING, String.format("failed to delete runtime '%s'", runtimeId), e);
        errors++;
      }
    }
    log.info(
        String.format(
            "deleted %d old runtimes and %d idle runtimes (with %d errors) "
                + "of %d total runtimes (%d of which were idle)",
            activeDeletes, unusedDeletes, errors, listRuntimeResponses.size(), idles));
    if (errors > 0) {
      throw new ServerErrorException(String.format("%d runtime deletion calls failed", errors));
    }
    return ResponseEntity.noContent().build();
  }

  private static String formatDuration(Duration d) {
    if ((d.toHours() % 24) == 0) {
      return String.format("%dd", d.toDays());
    }
    return String.format("%dd %dh", d.toDays(), d.toHours() % 24);
  }
}
