package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.UserBQCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to call the {@link FreeTierBillingService} with batches of users. This ensures that
 * FreeTierBillingService will commit the transaction for a smaller batch of users instead of
 * processing all users in one transaction and eventually timing out. See RW-6280
 */
@Service
public class FreeTierBillingBatchUpdateService {

  private static final Logger logger =
      Logger.getLogger(FreeTierBillingBatchUpdateService.class.getName());

  private final UserDao userDao;
  private final FreeTierBillingService freeTierBillingService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final BigQueryService bigQueryService;

  private static final int MIN_USERS_BATCH = 5;
  private static final int MAX_USERS_BATCH = 999;
  public static final Range<Integer> batchSizeRange =
      Range.closed(MIN_USERS_BATCH, MAX_USERS_BATCH);

  @Autowired
  public FreeTierBillingBatchUpdateService(
      UserDao userDao,
      FreeTierBillingService freeTierBillingService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      BigQueryService bigQueryService) {
    this.userDao = userDao;
    this.freeTierBillingService = freeTierBillingService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  /**
   * Takes in List of : user and all BQ cost of workspace for the user
   *
   * @param userBQCostReq
   */
  public void checkAndAlertFreeTierBillingUsage(List<UserBQCost> userBQCostReq) {
    Map<String, Double> userWorkspaceBQCosts = new HashMap<String, Double>();

    userBQCostReq.forEach(
        userBQCost -> userWorkspaceBQCosts.putAll(userBQCost.getWorkspaceBQCost()));

    Set<DbUser> dbUserSet =
        userBQCostReq.stream()
            .map((userCost) -> userDao.findUserByUserId(userCost.getUserId()))
            .collect(Collectors.toSet());

    freeTierBillingService.checkFreeTierBillingUsageForUsers(dbUserSet, userWorkspaceBQCosts);
  }

  /**
   * 1- Get users who have active free tier workspaces 2- Iterate over these users in batches of X
   * and find the cost of their workspaces before/after
   */
  public void checkFreeTierBillingUsage() {
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
      freeTierBillingService.checkFreeTierBillingUsageForUsers(
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
    // If the batch size is somehow not configured or incorrectly configured, return the minimum
    // batch size
    if (freeTierCronUserBatchSize == null || freeTierCronUserBatchSize <= 0) {
      return batchSizeRange.lowerEndpoint();
    }
    // If it's configured correctly within range, then just return it
    if (batchSizeRange.contains(freeTierCronUserBatchSize)) {
      return freeTierCronUserBatchSize;
    } else {
      // Otherwise, check if it's lower than the min, then take the min, or if it's higher, then
      // take the max
      return freeTierCronUserBatchSize < batchSizeRange.lowerEndpoint()
          ? batchSizeRange.lowerEndpoint()
          : batchSizeRange.upperEndpoint();
    }
  }
}
