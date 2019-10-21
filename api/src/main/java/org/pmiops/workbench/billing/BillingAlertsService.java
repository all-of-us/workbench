package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Streams;
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
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingAlertsService {

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingAlertsService(
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
    final Map<Workspace, Double> workspaceCosts = getFreeTierWorkspaceCosts();

    final Set<User> expiredCreditsUsers =
        workspaceCosts.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getKey().getCreator(), Collectors.summingDouble(Entry::getValue)))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > getUserFreeTierLimit(entry.getKey()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    for (User expiredUser : expiredCreditsUsers) {
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

  private Map<Workspace, Double> getFreeTierWorkspaceCosts() {

    final Map<String, Workspace> workspacesIndexedByProject =
        // don't record cost for OLD or MIGRATED workspaces - only NEW
        workspaceDao.findAllByBillingMigrationStatus(BillingMigrationStatus.NEW).stream()
            .collect(Collectors.toMap(Workspace::getWorkspaceNamespace, Function.identity()));

    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT project.id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` GROUP BY project.id ORDER BY cost desc;")
            .build();

    return Streams.stream(bigQueryService.executeQuery(queryConfig).getValues())
        .filter(
            tableRow -> workspacesIndexedByProject.containsKey(tableRow.get("id").getStringValue()))
        .collect(
            Collectors.groupingBy(
                tableRow -> workspacesIndexedByProject.get(tableRow.get("id").getStringValue()),
                Collectors.summingDouble(tableRow -> tableRow.get("cost").getDoubleValue())));
  }

  private Double getUserFreeTierLimit(User user) {
    if (user.getFreeTierCreditsLimitOverride() != null) {
      return user.getFreeTierCreditsLimitOverride();
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsLimit;
  }
}
