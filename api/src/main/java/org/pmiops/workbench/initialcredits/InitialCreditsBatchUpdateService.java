package org.pmiops.workbench.initialcredits;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to call the {@link InitialCreditsService} with batches of users. This ensures that
 * InitialCreditsBatchUpdateService will commit the transaction for a smaller batch of users instead
 * of processing all users in one transaction and eventually timing out. See RW-6280
 */
@Service
public class InitialCreditsBatchUpdateService {
  private static final Logger log =
      Logger.getLogger(InitialCreditsBatchUpdateService.class.getName());

  private final BigQueryService bigQueryService;
  private final InitialCreditsService initialCreditsService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public InitialCreditsBatchUpdateService(
      BigQueryService bigQueryService,
      InitialCreditsService initialCreditsService,
      Provider<Stopwatch> stopwatchProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      WorkspaceDao workspaceDao) {
    this.bigQueryService = bigQueryService;
    this.initialCreditsService = initialCreditsService;
    this.userDao = userDao;
    this.stopwatchProvider = stopwatchProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
  }

  /**
   * Takes in List of user Id and then find its BQ Cost
   *
   * @param userIdList
   */
  public void checkInitialCreditsUsage(List<Long> userIdList) {

    // Get the list of users from the database based on the userIdList.
    List<DbUser> users = userDao.findUsersByUserIdIn(userIdList);

    // This returns a map of google project id to its live cost in BigQuery.
    Map<String, Double> terraWorkspacesLiveCosts = findTerraWorkspacesLiveCosts(userIdList);

    // This is a map of user_id to its live total cost in BigQuery for VWB projects. We don't need
    // the google project id here because we don't store it anywhere in the database and therefore
    // it doesn't give us any information.
    Map<Long, Double> vwbUserLiveCosts = findVwbUsersLiveCosts(users);

    Set<DbUser> dbUserSet = Sets.newHashSet(users);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        dbUserSet, terraWorkspacesLiveCosts, vwbUserLiveCosts);
  }

  private @NotNull Map<String, Double> findTerraWorkspacesLiveCosts(List<Long> userIdList) {
    Set<String> googleProjects = workspaceDao.getWorkspaceGoogleProjectsForCreators(userIdList);
    Stopwatch stopwatch = stopwatchProvider.get().start();
    Map<String, Double> userWorkspaceCosts =
        getAllTerraWorkspaceCostsFromBQ().entrySet().stream()
            .filter(entry -> googleProjects.contains(entry.getKey()))
            .collect(
                Collectors.groupingBy(Entry::getKey, Collectors.summingDouble(Entry::getValue)));
    Duration elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: Filtered %d workspace cost entries from BigQuery in %s",
            userWorkspaceCosts.size(), formatDurationPretty(elapsed)));
    return userWorkspaceCosts;
  }

  /** Return a map of user_id -> total_cost for VWB pods */
  private Map<Long, Double> findVwbUsersLiveCosts(List<DbUser> users) {
    // Filter and collect active VWB user pods
    Map<String, DbVwbUserPod> activePodIdToUserPodMap =
        users.stream()
            .map(DbUser::getVwbUserPod)
            .filter(Objects::nonNull)
            .filter(DbVwbUserPod::isInitialCreditsActive)
            .collect(Collectors.toMap(DbVwbUserPod::getVwbPodId, pod -> pod));

    // Extract the set of active VWB pod IDs
    Set<String> activeVwbPodIds = activePodIdToUserPodMap.keySet();

    // Map VWB project costs to user IDs
    return getAllVWBProjectCostsFromBQ().entrySet().stream()
        .filter(entry -> !activeVwbPodIds.contains(entry.getKey()))
        .collect(
            Collectors.toMap(
                entry -> activePodIdToUserPodMap.get(entry.getKey()).getUser().getUserId(),
                Entry::getValue));
  }

  private Map<String, Double> getAllTerraWorkspaceCostsFromBQ() {
    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` WHERE id IS NOT NULL "
                    + "GROUP BY id ORDER BY cost desc;")
            .build();

    final Map<String, Double> liveCostByWorkspace = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String googleProject = tableRow.get("id").getStringValue();
      liveCostByWorkspace.put(googleProject, tableRow.get("cost").getDoubleValue());
    }

    return liveCostByWorkspace;
  }

  /**
   * Gets all VWB project costs from BigQuery, the method aggregates costs by vwb_pod_id.
   *
   * @return a map of vwb_pod_id to total cost.
   */
  private Map<String, Double> getAllVWBProjectCostsFromBQ() {
    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT "
                    + "          id, "
                    + "          SUM(cost) AS total_cost, "
                    + "          MAX(CASE WHEN project_labels.key = 'vwb_pod_id' THEN project_labels.value ELSE NULL END) AS vwb_pod_id, "
                    + "          MAX(CASE WHEN project_labels.key = 'vwb_workspace_id' THEN project_labels.value ELSE NULL END) AS vwb_workspace_id FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` "
                    + "      LEFT JOIN "
                    + "          UNNEST(labels) AS project_labels "
                    + "      WHERE "
                    + "          EXISTS(SELECT 1 FROM UNNEST(tags) AS t WHERE t.key = 'env' AND t.value = 'prod') "
                    + "      GROUP BY "
                    + "          id ORDER BY cost desc;")
            .build();

    // Group by the pod
    final Map<String, Double> costByVwbPodId = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String vwbPodId =
          tableRow.get("vwb_pod_id").isNull() ? null : tableRow.get("vwb_pod_id").getStringValue();
      final double totalCost = tableRow.get("total_cost").getDoubleValue();
      if (vwbPodId != null) {
        costByVwbPodId.merge(vwbPodId, totalCost, Double::sum);
      }
    }

    return costByVwbPodId;
  }
}
