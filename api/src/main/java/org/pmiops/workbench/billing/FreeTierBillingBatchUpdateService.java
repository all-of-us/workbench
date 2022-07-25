package org.pmiops.workbench.billing;

import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.StreamUtils;
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

  private static final int MIN_USERS_BATCH = 5;
  private static final int MAX_USERS_BATCH = 999;

  @Autowired
  public FreeTierBillingBatchUpdateService(
      UserDao userDao,
      FreeTierBillingService freeTierBillingService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userDao = userDao;
    this.freeTierBillingService = freeTierBillingService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * 1- Get users who have active free tier workspaces 2- Iterate over these users in batches of X
   * and find the cost of their workspaces before/after
   */
  public void checkFreeTierBillingUsage() {
    logger.info("Checking Free Tier Billing usage - start");

    Iterable<DbUser> freeTierActiveWorkspaceCreators = userDao.findAll();
    long numberOfUsers =
        StreamUtils.createStreamFromIterator(freeTierActiveWorkspaceCreators.iterator()).count();

    for (List<DbUser> usersPartition :
        Iterables.partition(freeTierActiveWorkspaceCreators, getFreeTierCronUserBatchSize())) {
      logger.info(
          String.format(
              "Processing users batch of size/total: %d/%d", usersPartition.size(), numberOfUsers));
      freeTierBillingService.checkFreeTierBillingUsageForUsers(new HashSet<>(usersPartition));
    }

    logger.info("Checking Free Tier Billing usage - finish");
  }

  private int getFreeTierCronUserBatchSize() {
    Integer freeTierCronUserBatchSize =
        workbenchConfigProvider.get().billing.freeTierCronUserBatchSize;
    logger.info(String.format("freeTierCronUserBatchSize is %d", freeTierCronUserBatchSize));
    return Math.min(
        MAX_USERS_BATCH,
        freeTierCronUserBatchSize == null || freeTierCronUserBatchSize <= 0
            ? MIN_USERS_BATCH
            : freeTierCronUserBatchSize);
  }
}
