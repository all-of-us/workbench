package org.pmiops.workbench.initialcredits;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import jakarta.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to call the {@link InitialCreditsService} with batches of users. This ensures that
 * FreeTierBillingService will commit the transaction for a smaller batch of users instead of
 * processing all users in one transaction and eventually timing out. See RW-6280
 */
@Service
public class InitialCreditsBatchUpdateService {

  private static final Logger logger =
      Logger.getLogger(InitialCreditsBatchUpdateService.class.getName());

  private final UserDao userDao;
  private final InitialCreditsService initialCreditsService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final BigQueryService bigQueryService;

  private final GoogleProjectPerCostDao googleProjectPerCostDao;

  private final WorkspaceDao workspaceDao;

  private static final int MIN_USERS_BATCH = 5;
  private static final int MAX_USERS_BATCH = 999;
  public static final Range<Integer> batchSizeRange =
      Range.closed(MIN_USERS_BATCH, MAX_USERS_BATCH);

  @Autowired
  public InitialCreditsBatchUpdateService(
      GoogleProjectPerCostDao googleProjectPerCostDao,
      UserDao userDao,
      InitialCreditsService initialCreditsService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceDao workspaceDao,
      BigQueryService bigQueryService) {
    this.googleProjectPerCostDao = googleProjectPerCostDao;
    this.userDao = userDao;
    this.initialCreditsService = initialCreditsService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
    this.workspaceDao = workspaceDao;
  }

  /**
   * Takes in List of user Id and then find its BQ Cost
   *
   * @param userIdList
   */
  public void checkInitialCreditsUsage(List<Long> userIdList) {
    Set<String> googleProjectsForUserSet = workspaceDao.getGoogleProjectForUserList(userIdList);

    List<DbGoogleProjectPerCost> googleProjectPerCostList =
        (List<DbGoogleProjectPerCost>)
            googleProjectPerCostDao.findAllByGoogleProjectId(googleProjectsForUserSet);

    // Create Map Key: googleProject and value: cost
    Map<String, Double> userWorkspaceBQCosts =
        googleProjectPerCostList.stream()
            .collect(
                Collectors.toMap(
                    DbGoogleProjectPerCost::getGoogleProjectId, DbGoogleProjectPerCost::getCost));

    Set<DbUser> dbUserSet =
        userIdList.stream().map(userDao::findUserByUserId).collect(Collectors.toSet());

    initialCreditsService.checkInitialCreditsUsageForUsers(dbUserSet, userWorkspaceBQCosts);
  }

  /**
   * 1- Get users who have active free tier workspaces 2- Iterate over these users in batches of X
   * and find the cost of their workspaces before/after
   */
  public void checkInitialCreditsUsage() {
    logger.info("Checking Free Tier Billing usage - start");

    Iterable<DbUser> freeTierActiveWorkspaceCreators = userDao.findAll();
    long numberOfUsers = Iterators.size(freeTierActiveWorkspaceCreators.iterator());
    int count = 0;

    Map<String, Double> allBQCosts = getFreeTierWorkspaceCostsFromBQ();

    logger.info(String.format("Retrieved all BQ costs, size is: %d", allBQCosts.size()));

    for (List<DbUser> usersPartition :
        Iterables.partition(
            freeTierActiveWorkspaceCreators, freeTierCronUserBatchSizeFromConfig())) {
      logger.info(
          String.format(
              "Processing users batch of size/total: %d/%d. Current iteration is: %d",
              usersPartition.size(), numberOfUsers, count++));
      initialCreditsService.checkInitialCreditsUsageForUsers(
          new HashSet<>(usersPartition), allBQCosts);
    }

    logger.info("Checking Free Tier Billing usage - finish");
  }

  public Map<String, Double> getFreeTierWorkspaceCostsFromBQ() {
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

  private int freeTierCronUserBatchSizeFromConfig() {
    Integer freeTierCronUserBatchSize =
        workbenchConfigProvider.get().billing.freeTierCronUserBatchSize;
    logger.info(String.format("freeTierCronUserBatchSize is %d", freeTierCronUserBatchSize));

    if (freeTierCronUserBatchSize == null || !batchSizeRange.contains(freeTierCronUserBatchSize)) {
      freeTierCronUserBatchSize =
          freeTierCronUserBatchSize != null
                  && freeTierCronUserBatchSize < batchSizeRange.lowerEndpoint()
              ? batchSizeRange.lowerEndpoint()
              : batchSizeRange.upperEndpoint();
    }

    return freeTierCronUserBatchSize;
  }
}
