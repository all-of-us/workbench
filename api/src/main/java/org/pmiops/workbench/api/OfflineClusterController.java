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
import org.pmiops.workbench.model.CheckClustersResponse;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline cluster API. Handles cronjobs for cleanup and upgrade of older clusters. Methods here
 * should be access restricted as, unlike ClusterController, these endpoints run as the Workbench
 * service account.
 */
@RestController
public class OfflineClusterController implements OfflineClusterApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineClusterController.class.getName());

  // This is temporary while we wait for Leonardo autopause to rollout. Once
  // available, we should instead take a cluster status of STOPPED to trigger
  // idle deletion.
  private static final int IDLE_AFTER_HOURS = 3;

  private final Provider<ClusterApi> clusterApiProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  @Autowired
  OfflineClusterController(
      @Qualifier(NotebooksConfig.SERVICE_CLUSTER_API) Provider<ClusterApi> clusterApiProvider,
      Provider<WorkbenchConfig> configProvider,
      Clock clock) {
    this.clock = clock;
    this.configProvider = configProvider;
    this.clusterApiProvider = clusterApiProvider;
  }

  /**
   * checkClusters deletes older clusters in order to force an upgrade on the next researcher login.
   * This method is meant to be restricted to invocation by App Engine cron.
   *
   * <p>The cluster deletion policy here aims to strike a balance between enforcing upgrades, cost
   * savings, and minimizing user disruption. To this point, our goal is to only upgrade idle
   * clusters if possible, as doing so gives us an assurance that a researcher is not actively using
   * it. We delete clusters in the following cases:
   *
   * <p>1. It exceeds the max cluster age. Per environment, but O(weeks). 2. It is idle and exceeds
   * the max idle cluster age. Per environment, smaller than (1).
   *
   * <p>As an App Engine cron endpoint, the runtime of this method may not exceed 10 minutes.
   */
  @Override
  public ResponseEntity<CheckClustersResponse> checkClusters() {
    Instant now = clock.instant();
    WorkbenchConfig config = configProvider.get();
    Duration maxAge = Duration.ofDays(config.firecloud.clusterMaxAgeDays);
    Duration idleMaxAge = Duration.ofDays(config.firecloud.clusterIdleMaxAgeDays);

    ClusterApi clusterApi = clusterApiProvider.get();
    List<ListClusterResponse> clusters;
    try {
      clusters = clusterApi.listClusters(null, false);
    } catch (ApiException e) {
      throw ExceptionUtils.convertNotebookException(e);
    }

    int errors = 0;
    int idles = 0;
    int activeDeletes = 0;
    int unusedDeletes = 0;
    for (ListClusterResponse listClusterResponse : clusters) {
      String clusterId = listClusterResponse.getGoogleProject() + "/" + listClusterResponse.getClusterName();
      Cluster c;
      try {
        // Refetch the cluster to ensure freshness as this iteration may take
        // some time.
        c = clusterApi.getCluster(listClusterResponse.getGoogleProject(), listClusterResponse.getClusterName());
      } catch (ApiException e) {
        log.log(Level.WARNING, String.format("failed to refetch cluster '%s'", clusterId), e);
        errors++;
        continue;
      }
      if (ClusterStatus.UNKNOWN.equals(c.getStatus()) || c.getStatus() == null) {
        log.warning(String.format("unknown cluster status for cluster '%s'", clusterId));
        continue;
      }
      if (!ClusterStatus.RUNNING.equals(c.getStatus())
          && !ClusterStatus.STOPPED.equals(c.getStatus())) {
        // For now, we only handle running or stopped (suspended) clusters.
        continue;
      }

      Instant lastUsed = Instant.parse(c.getDateAccessed());
      boolean isIdle = Duration.between(lastUsed, now).toHours() > IDLE_AFTER_HOURS;
      if (isIdle) {
        idles++;
      }

      Instant created = Instant.parse(c.getCreatedDate());
      Duration age = Duration.between(created, now);
      if (age.toMillis() > maxAge.toMillis()) {
        log.info(
            String.format(
                "deleting cluster '%s', exceeded max lifetime @ %s (>%s)",
                clusterId, formatDuration(age), formatDuration(maxAge)));
        activeDeletes++;
      } else if (isIdle && age.toMillis() > idleMaxAge.toMillis()) {
        log.info(
            String.format(
                "deleting cluster '%s', idle with age %s (>%s)",
                clusterId, formatDuration(age), formatDuration(idleMaxAge)));
        unusedDeletes++;
      } else {
        // Don't delete.
        continue;
      }
      try {
        clusterApi.deleteCluster(c.getGoogleProject(), c.getClusterName());
      } catch (ApiException e) {
        log.log(Level.WARNING, String.format("failed to delete cluster '%s'", clusterId), e);
        errors++;
      }
    }
    log.info(
        String.format(
            "deleted %d old clusters and %d idle clusters (with %d errors) "
                + "of %d total clusters (%d of which were idle)",
            activeDeletes, unusedDeletes, errors, clusters.size(), idles));
    if (errors > 0) {
      throw new ServerErrorException(String.format("%d cluster deletion calls failed", errors));
    }
    return ResponseEntity.ok(
        new CheckClustersResponse().clusterDeletionCount(activeDeletes + unusedDeletes - errors));
  }

  private static String formatDuration(Duration d) {
    if ((d.toHours() % 24) == 0) {
      return String.format("%dd", d.toDays());
    }
    return String.format("%dd %dh", d.toDays(), d.toHours() % 24);
  }
}
