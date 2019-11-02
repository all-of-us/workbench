package org.pmiops.workbench.api

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.exceptions.ExceptionUtils
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.model.CheckClustersResponse
import org.pmiops.workbench.notebooks.ApiException
import org.pmiops.workbench.notebooks.NotebooksConfig
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.model.Cluster
import org.pmiops.workbench.notebooks.model.ClusterStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * Offline cluster API. Handles cronjobs for cleanup and upgrade of older clusters. Methods here
 * should be access restricted as, unlike ClusterController, these endpoints run as the Workbench
 * service account.
 */
@RestController
class OfflineClusterController @Autowired
internal constructor(
        @param:Qualifier(NotebooksConfig.SERVICE_CLUSTER_API) private val clusterApiProvider: Provider<ClusterApi>,
        private val configProvider: Provider<WorkbenchConfig>,
        private val clock: Clock) : OfflineClusterApiDelegate {

    /**
     * checkClusters deletes older clusters in order to force an upgrade on the next researcher login.
     * This method is meant to be restricted to invocation by App Engine cron.
     *
     *
     * The cluster deletion policy here aims to strike a balance between enforcing upgrades, cost
     * savings, and minimizing user disruption. To this point, our goal is to only upgrade idle
     * clusters if possible, as doing so gives us an assurance that a researcher is not actively using
     * it. We delete clusters in the following cases:
     *
     *
     * 1. It exceeds the max cluster age. Per environment, but O(weeks). 2. It is idle and exceeds
     * the max idle cluster age. Per environment, smaller than (1).
     *
     *
     * As an App Engine cron endpoint, the runtime of this method may not exceed 10 minutes.
     */
    fun checkClusters(): ResponseEntity<CheckClustersResponse> {
        val now = clock.instant()
        val config = configProvider.get()
        val maxAge = Duration.ofDays(config.firecloud.clusterMaxAgeDays.toLong())
        val idleMaxAge = Duration.ofDays(config.firecloud.clusterIdleMaxAgeDays.toLong())

        val clusterApi = clusterApiProvider.get()
        val clusters: List<Cluster>
        try {
            clusters = clusterApi.listClusters(null, false)
        } catch (e: ApiException) {
            throw ExceptionUtils.convertNotebookException(e)
        }

        var errors = 0
        var idles = 0
        var activeDeletes = 0
        var unusedDeletes = 0
        for (c in clusters) {
            val clusterId = c.getGoogleProject() + "/" + c.getClusterName()
            try {
                // Refetch the cluster to ensure freshness as this iteration may take
                // some time.
                c = clusterApi.getCluster(c.getGoogleProject(), c.getClusterName())
            } catch (e: ApiException) {
                log.log(Level.WARNING, String.format("failed to refetch cluster '%s'", clusterId), e)
                errors++
                continue
            }

            if (ClusterStatus.UNKNOWN.equals(c.getStatus()) || c.getStatus() == null) {
                log.warning(String.format("unknown cluster status for cluster '%s'", clusterId))
                continue
            }
            if (!ClusterStatus.RUNNING.equals(c.getStatus()) && !ClusterStatus.STOPPED.equals(c.getStatus())) {
                // For now, we only handle running or stopped (suspended) clusters.
                continue
            }

            val lastUsed = Instant.parse(c.getDateAccessed())
            val isIdle = Duration.between(lastUsed, now).toHours() > IDLE_AFTER_HOURS
            if (isIdle) {
                idles++
            }

            val created = Instant.parse(c.getCreatedDate())
            val age = Duration.between(created, now)
            if (age.toMillis() > maxAge.toMillis()) {
                log.info(
                        String.format(
                                "deleting cluster '%s', exceeded max lifetime @ %s (>%s)",
                                clusterId, formatDuration(age), formatDuration(maxAge)))
                activeDeletes++
            } else if (isIdle && age.toMillis() > idleMaxAge.toMillis()) {
                log.info(
                        String.format(
                                "deleting cluster '%s', idle with age %s (>%s)",
                                clusterId, formatDuration(age), formatDuration(idleMaxAge)))
                unusedDeletes++
            } else {
                // Don't delete.
                continue
            }
            try {
                clusterApi.deleteCluster(c.getGoogleProject(), c.getClusterName())
            } catch (e: ApiException) {
                log.log(Level.WARNING, String.format("failed to delete cluster '%s'", clusterId), e)
                errors++
            }

        }
        log.info(
                String.format(
                        "deleted %d old clusters and %d idle clusters (with %d errors) " + "of %d total clusters (%d of which were idle)",
                        activeDeletes, unusedDeletes, errors, clusters.size, idles))
        if (errors > 0) {
            throw ServerErrorException(String.format("%d cluster deletion calls failed", errors))
        }
        return ResponseEntity.ok(
                CheckClustersResponse().clusterDeletionCount(activeDeletes + unusedDeletes - errors))
    }

    companion object {
        private val log = Logger.getLogger(OfflineClusterController::class.java.name)

        // This is temporary while we wait for Leonardo autopause to rollout. Once
        // available, we should instead take a cluster status of STOPPED to trigger
        // idle deletion.
        private val IDLE_AFTER_HOURS = 3

        private fun formatDuration(d: Duration): String {
            return if (d.toHours() % 24 == 0L) {
                String.format("%dd", d.toDays())
            } else String.format("%dd %dh", d.toDays(), d.toHours() % 24)
        }
    }
}
