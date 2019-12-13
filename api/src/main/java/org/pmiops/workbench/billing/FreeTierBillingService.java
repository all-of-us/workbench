package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FreeTierBillingService {

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      NotificationService notificationService,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void checkFreeTierBillingUsage() {
    final Map<DbWorkspace, Double> workspaceCosts = getFreeTierWorkspaceCosts();

    final Set<DbUser> expiredCreditsUsers =
        workspaceCosts.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getKey().getCreator(), Collectors.summingDouble(Entry::getValue)))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > getUserFreeTierDollarLimit(entry.getKey()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    for (DbUser expiredUser : expiredCreditsUsers) {
      notificationService.alertUser(expiredUser, "You have exceeded your free tier credits.");
    }

    workspaceCosts.forEach(
        (workspace, cost) -> {
          workspaceFreeTierUsageDao.updateCost(workspace, cost);

          BillingStatus status =
              expiredCreditsUsers.contains(workspace.getCreator())
                  ? BillingStatus.INACTIVE
                  : BillingStatus.ACTIVE;
          workspaceDao.updateBillingStatus(workspace.getWorkspaceId(), status);
        });
  }

  private Map<DbWorkspace, Double> getFreeTierWorkspaceCosts() {

    final Map<String, DbWorkspace> workspacesIndexedByProject =
        // don't record cost for OLD or MIGRATED workspaces - only NEW
        workspaceDao.findAllByBillingMigrationStatus(BillingMigrationStatus.NEW).stream()
            .collect(Collectors.toMap(DbWorkspace::getWorkspaceNamespace, Function.identity()));

    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT project.id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` WHERE project.id IS NOT NULL "
                    + "GROUP BY project.id ORDER BY cost desc;")
            .build();

    final Map<DbWorkspace, Double> workspaceCosts = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String project = tableRow.get("id").getStringValue();
      if (workspacesIndexedByProject.containsKey(project)) {
        workspaceCosts.put(
            workspacesIndexedByProject.get(project), tableRow.get("cost").getDoubleValue());
      }
    }

    return workspaceCosts;
  }

  // Retrieve the user's total free tier usage from the DB by summing across Workspaces.
  // This is not live BigQuery data: it is only as recent as the last
  // checkFreeTierBillingUsage Cron job, recorded as last_update_time in the DB.
  public Double getUserCachedFreeTierUsage(DbUser user) {
    return workspaceFreeTierUsageDao.totalCostByUser(user);
  }

  public Double getUserFreeTierDollarLimit(DbUser user) {
    final Double override = user.getFreeTierCreditsLimitDollarsOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDollarLimit;
  }

  public Short getUserFreeTierDaysLimit(DbUser user) {
    final Short override = user.getFreeTierCreditsLimitDaysOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDaysLimit;
  }
}
