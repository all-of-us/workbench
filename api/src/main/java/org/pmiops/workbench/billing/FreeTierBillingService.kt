package org.pmiops.workbench.billing

import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryJobConfiguration
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.function.Function
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.model.BillingStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FreeTierBillingService @Autowired
constructor(
        private val bigQueryService: BigQueryService,
        private val notificationService: NotificationService,
        private val workspaceDao: WorkspaceDao,
        private val workspaceFreeTierUsageDao: WorkspaceFreeTierUsageDao,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) {

    private// don't record cost for OLD or MIGRATED workspaces - only NEW
    val freeTierWorkspaceCosts: Map<Workspace, Double>
        get() {

            val workspacesIndexedByProject = workspaceDao.findAllByBillingMigrationStatus(BillingMigrationStatus.NEW).stream()
                    .collect<Map<String, Workspace>, Any>(Collectors.toMap(Function { it.workspaceNamespace }, Function.identity()))

            val queryConfig = QueryJobConfiguration.newBuilder(
                    "SELECT project.id, SUM(cost) cost FROM `"
                            + workbenchConfigProvider.get().billing.exportBigQueryTable
                            + "` WHERE project.id IS NOT NULL "
                            + "GROUP BY project.id ORDER BY cost desc;")
                    .build()

            val workspaceCosts = HashMap<Workspace, Double>()
            for (tableRow in bigQueryService.executeQuery(queryConfig).values) {
                val project = tableRow.get("id").stringValue
                if (workspacesIndexedByProject.containsKey(project)) {
                    workspaceCosts[workspacesIndexedByProject[project]] = tableRow.get("cost").doubleValue
                }
            }

            return workspaceCosts
        }

    fun checkFreeTierBillingUsage() {
        val workspaceCosts = freeTierWorkspaceCosts

        val expiredCreditsUsers = workspaceCosts.entries.stream()
                .collect<Map<User, Double>, Any>(
                        Collectors.groupingBy<Entry<Workspace, Double>, User, Any, Double>(
                                { e -> e.key.creator }, Collectors.summingDouble(ToDoubleFunction<Entry<Workspace, Double>> { it.value })))
                .entries
                .stream()
                .filter { entry -> entry.value > getUserFreeTierLimit(entry.key) }
                .map { it.key }
                .collect<Set<User>, Any>(Collectors.toSet())

        for (expiredUser in expiredCreditsUsers) {
            notificationService.alertUser(expiredUser, "You have exceeded your free tier credits.")
        }

        workspaceCosts.forEach { (workspace, cost) ->
            workspaceFreeTierUsageDao.updateCost(workspace, cost)

            val status = if (expiredCreditsUsers.contains(workspace.creator))
                BillingStatus.INACTIVE
            else
                BillingStatus.ACTIVE
            workspaceDao.updateBillingStatus(workspace.workspaceId, status)
        }
    }

    // Retrieve the user's total free tier usage from the DB by summing across Workspaces.
    // This is not live BigQuery data: it is only as recent as the last
    // checkFreeTierBillingUsage Cron job, recorded as last_update_time in the DB.
    fun getUserCachedFreeTierUsage(user: User): Double? {
        return workspaceFreeTierUsageDao.totalCostByUser(user)
    }

    fun getUserFreeTierLimit(user: User): Double? {
        return if (user.freeTierCreditsLimitOverride != null) {
            user.freeTierCreditsLimitOverride
        } else workbenchConfigProvider.get().billing.defaultFreeCreditsLimit

    }
}
