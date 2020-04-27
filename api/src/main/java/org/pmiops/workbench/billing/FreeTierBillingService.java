package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.mail.MessagingException;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Methods relating to Free Tier credit usage and limits */
@Service
public class FreeTierBillingService {

  private final BigQueryService bigQueryService;
  private final MailService mailService;

  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final Logger logger = Logger.getLogger(FreeTierBillingService.class.getName());

  // somewhat arbitrary - 1 per million
  private static final double COST_COMPARISON_TOLERANCE = 0.000001;
  private static final double COST_FRACTION_TOLERANCE = 0.000001;

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      MailService mailService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.mailService = mailService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public double getWorkspaceFreeTierBillingUsage(DbWorkspace dbWorkspace) {
    DbWorkspaceFreeTierUsage dbWorkspaceFreeTierUsage =
        workspaceFreeTierUsageDao.findOneByWorkspace(dbWorkspace);
    if (dbWorkspaceFreeTierUsage == null) {
      return 0;
    }
    return dbWorkspaceFreeTierUsage.getCost();
  }

  /**
   * Check whether users have incurred sufficient cost or time in their workspaces to trigger alerts
   * due to passing thresholds or exceeding limits
   */
  public void checkFreeTierBillingUsage() {
    // retrieve the costs stored in the DB from the last time this was run
    final Map<DbUser, Double> previousUserCosts = workspaceFreeTierUsageDao.getUserCostMap();

    // retrieve current workspace costs from BigQuery and store in the DB
    final Map<DbWorkspace, Double> workspaceCosts = getFreeTierWorkspaceCostsFromBQ();
    workspaceCosts.forEach(workspaceFreeTierUsageDao::updateCost);

    // sum current workspace costs by workspace creator
    final Map<DbUser, Double> userCosts =
        workspaceCosts.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getKey().getCreator(), Collectors.summingDouble(Entry::getValue)));

    // check cost and time thresholds for the relevant users

    // collect previously-expired and currently-expired users by cost and time
    // for users which are expired: alert only if they were not expired previously
    // for users which are not yet expired:
    //    check for intermediate thresholds and alert, possibly for both cost and time

    final Set<DbUser> previouslyExpiredUsers = getExpiredUsersFromDb();

    final Set<DbUser> expiredUsers =
        userCosts.entrySet().stream()
            .filter(e -> expiredByCost(e.getKey(), e.getValue()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    final Set<DbUser> newlyExpiredUsers = Sets.difference(expiredUsers, previouslyExpiredUsers);
    for (final DbUser user : newlyExpiredUsers) {
      try {
        mailService.alertUserFreeTierExpiration(user);
      } catch (final MessagingException e) {
        logger.log(Level.WARNING, e.getMessage());
      }
      deactivateUserWorkspaces(user);
    }

    final Set<DbUser> usersWithNonNullRegistration =
        userDao.findByFirstRegistrationCompletionTimeNotNull();
    final Set<DbUser> usersToThresholdCheck =
        Sets.difference(usersWithNonNullRegistration, expiredUsers);

    sendAlertsForCostThresholds(usersToThresholdCheck, previousUserCosts, userCosts);
  }

  private int compareCosts(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_COMPARISON_TOLERANCE);
  }

  private int compareCostFractions(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_FRACTION_TOLERANCE);
  }

  private boolean expiredByCost(final DbUser user, final double currentCost) {
    return compareCosts(currentCost, getUserFreeTierDollarLimit(user)) > 0;
  }

  private void deactivateUserWorkspaces(final DbUser user) {
    final Set<DbWorkspace> toDeactivate = workspaceDao.findAllByCreator(user);
    for (final DbWorkspace workspace : toDeactivate) {
      workspaceDao.updateBillingStatus(workspace.getWorkspaceId(), BillingStatus.INACTIVE);
    }
  }

  private void sendAlertsForCostThresholds(
      Set<DbUser> usersToCheck,
      Map<DbUser, Double> previousUserCosts,
      Map<DbUser, Double> userCosts) {
    final List<Double> costThresholdsInDescOrder =
        workbenchConfigProvider.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    userCosts.forEach(
        (user, currentCost) -> {
          if (usersToCheck.contains(user)) {
            final double previousCost = previousUserCosts.getOrDefault(user, 0.0);
            maybeAlertOnCostThresholds(user, currentCost, previousCost, costThresholdsInDescOrder);
          }
        });
  }

  /**
   * Has this user passed a cost threshold between this check and the previous run?
   *
   * <p>Compare this user's total cost with that of the previous run, and trigger an alert if this
   * is the run which pushed it over a free credits threshold.
   *
   * @param user The user to check
   * @param currentCost The current total cost incurred by this user, according to BigQuery
   * @param previousCost The total cost incurred by this user at the time of the previous check, as
   *     stored in the database
   * @param thresholdsInDescOrder the cost alerting thresholds, in descending order
   */
  private void maybeAlertOnCostThresholds(
      DbUser user, double currentCost, double previousCost, List<Double> thresholdsInDescOrder) {
    final double limit = getUserFreeTierDollarLimit(user);
    final double remainingBalance = limit - currentCost;

    // this shouldn't happen, but it did (RW-4678)
    // alert if it happens again
    if (compareCosts(currentCost, previousCost) < 0) {
      String msg =
          String.format(
              "User %s (%s) has %f in total free tier spending in BigQuery, "
                  + "which is less than the %f previous spending we have recorded in the DB",
              user.getUsername(),
              Optional.ofNullable(user.getContactEmail()).orElse("NULL"),
              currentCost,
              previousCost);
      logger.warning(msg);
    }

    final double currentFraction = currentCost / limit;
    final double previousFraction = previousCost / limit;

    for (final double threshold : thresholdsInDescOrder) {
      if (compareCostFractions(currentFraction, threshold) > 0) {
        // only alert if we have not done so previously
        if (compareCostFractions(previousFraction, threshold) <= 0) {
          try {
            mailService.alertUserFreeTierDollarThreshold(
                user, threshold, currentCost, remainingBalance);
          } catch (final MessagingException e) {
            logger.log(Level.WARNING, e.getMessage());
          }
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }

  // we set Workspaces to INACTIVE when their creators have exceeded their free credits
  // so we can retrieve these users by querying for inactive workspaces
  private Set<DbUser> getExpiredUsersFromDb() {
    return workspaceDao.findAllCreatorsByBillingStatus(BillingStatus.INACTIVE);
  }

  private Map<DbWorkspace, Double> getFreeTierWorkspaceCostsFromBQ() {

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

  /**
   * Retrieve the user's total free tier usage from the DB by summing across the Workspaces they
   * have created. This is NOT live BigQuery data: it is only as recent as the last
   * checkFreeTierBillingUsage cron job, recorded as last_update_time in the DB.
   *
   * <p>Note: return value may be null, to enable direct assignment to the nullable Profile field
   *
   * @param user the user as represented in our database
   * @return the total USD amount spent in workspaces created by this user, represented as a double
   */
  @Nullable
  public Double getCachedFreeTierUsage(DbUser user) {
    return workspaceFreeTierUsageDao.totalCostByUser(user);
  }

  /**
   * Does this user have remaining free tier credits? Compare the user-specific free tier limit (may
   * be the system default) to the amount they have used.
   *
   * @param user the user as represented in our database
   * @return whether the user has remaining credits
   */
  public boolean userHasRemainingFreeTierCredits(DbUser user) {
    return Optional.ofNullable(getCachedFreeTierUsage(user))
        .map(usage -> compareCosts(getUserFreeTierDollarLimit(user), usage) > 0)
        .orElse(true);
  }

  /**
   * Retrieve the Free Tier dollar limit actually applicable to this user: this user's override if
   * present, the environment's default if not
   *
   * @param user the user as represented in our database
   * @return the US dollar amount, represented as a double
   */
  public double getUserFreeTierDollarLimit(DbUser user) {
    return Optional.ofNullable(user.getFreeTierCreditsLimitDollarsOverride())
        .orElse(workbenchConfigProvider.get().billing.defaultFreeCreditsDollarLimit);
  }
}
