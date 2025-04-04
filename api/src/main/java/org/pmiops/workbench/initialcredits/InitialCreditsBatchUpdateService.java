package org.pmiops.workbench.initialcredits;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
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
    Set<String> googleProjects = workspaceDao.getWorkspaceGoogleProjectsForCreators(userIdList);

    Stopwatch stopwatch = stopwatchProvider.get().start();
    Map<String, Double> userWorkspaceCosts =
        getAllWorkspaceCostsFromBQ().entrySet().stream()
            .filter(entry -> googleProjects.contains(entry.getKey()))
            .collect(
                Collectors.groupingBy(
                    Entry::getKey, Collectors.summingDouble(Map.Entry::getValue)));
    Duration elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: Filtered %d workspace cost entries from BigQuery in %s",
            userWorkspaceCosts.size(), formatDurationPretty(elapsed)));

    Set<DbUser> dbUserSet =
        userIdList.stream().map(userDao::findUserByUserId).collect(Collectors.toSet());

    initialCreditsService.checkInitialCreditsUsageForUsers(dbUserSet, userWorkspaceCosts);
  }

  private Map<String, Double> getAllWorkspaceCostsFromBQ() {
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
}
