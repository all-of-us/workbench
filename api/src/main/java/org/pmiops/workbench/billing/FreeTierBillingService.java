package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;
import java.util.Collection;
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
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
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
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private static final Logger logger = Logger.getLogger(FreeTierBillingService.class.getName());

  // somewhat arbitrary - 1 per million
  private static final double COST_COMPARISON_TOLERANCE = 0.000001;
  private static final double COST_FRACTION_TOLERANCE = 0.000001;

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      MailService mailService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao) {
    this.bigQueryService = bigQueryService;
    this.mailService = mailService;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userServiceAuditor = userServiceAuditor;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
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
   * Check whether users have incurred sufficient cost in their workspaces to trigger alerts due to
   * passing thresholds or exceeding limits
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

    // check cost thresholds for the relevant users

    // collect previously-expired and currently-expired users
    // for users who are expired: alert only if they were not expired previously
    // for users who are not yet expired: check for intermediate thresholds and alert

    final Set<DbUser> previouslyExpiredUsers = getExpiredUsersFromDb();

    final Set<DbUser> expiredUsers =
        userCosts.entrySet().stream()
            .filter(e -> costAboveLimit(e.getKey(), e.getValue()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    final Set<DbUser> newlyExpiredUsers = Sets.difference(expiredUsers, previouslyExpiredUsers);
    for (final DbUser user : newlyExpiredUsers) {
      try {
        mailService.alertUserFreeTierExpiration(user);
      } catch (final MessagingException e) {
        logger.log(Level.WARNING, e.getMessage());
      }
      updateFreeTierWorkspacesStatus(user, BillingStatus.INACTIVE);
    }

    final List<DbUser> allUsers = userDao.findUsers();
    final Collection<DbUser> usersToThresholdCheck =
        CollectionUtils.subtract(allUsers, expiredUsers);

    sendAlertsForCostThresholds(usersToThresholdCheck, previousUserCosts, userCosts);
  }

  private int compareCosts(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_COMPARISON_TOLERANCE);
  }

  private int compareCostFractions(final double a, final double b) {
    return DoubleMath.fuzzyCompare(a, b, COST_FRACTION_TOLERANCE);
  }

  private boolean costAboveLimit(final DbUser user, final double currentCost) {
    return compareCosts(currentCost, getUserFreeTierDollarLimit(user)) > 0;
  }

  private boolean costsDiffer(final double a, final double b) {
    return compareCosts(a, b) != 0;
  }

  // TODO: move to DbWorkspace?  RW-5107
  private boolean isFreeTier(final DbWorkspace workspace) {
    return workspace
        .getBillingAccountName()
        .equals(workbenchConfigProvider.get().billing.freeTierBillingAccountName());
  }

  private void updateFreeTierWorkspacesStatus(final DbUser user, final BillingStatus status) {
    workspaceDao.findAllByCreator(user).stream()
        .filter(this::isFreeTier)
        .map(DbWorkspace::getWorkspaceId)
        .forEach(id -> workspaceDao.updateBillingStatus(id, status));
  }

  private void sendAlertsForCostThresholds(
      Collection<DbUser> usersToCheck,
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
            .collect(Collectors.toMap(DbWorkspace::getGoogleProject, Function.identity()));

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
    final double usage = Optional.ofNullable(getCachedFreeTierUsage(user)).orElse(0.0);
    return !costAboveLimit(user, usage);
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

  /**
   * Set a Free Tier dollar limit override value for this user, but only if the value to set differs
   * from the system default or the user has an existing override. If the user has no override and
   * the value to set it equal to the system default, retain the system default so this user's quota
   * continues to track it.
   *
   * <p>If this is greater than the user's total cost, set their workspaces to active. Note:
   * lowering the limit below total cost will NOT set the workspaces to inactive.
   * checkFreeTierBillingUsage() will do this as part of the next cron run.
   *
   * @param user the user as represented in our database
   * @param newDollarLimit the US dollar amount, represented as a double
   * @return whether an override was set
   */
  public boolean maybeSetDollarLimitOverride(DbUser user, double newDollarLimit) {
    final Double previousLimitMaybe = user.getFreeTierCreditsLimitDollarsOverride();

    if (previousLimitMaybe != null
        || costsDiffer(
            newDollarLimit, workbenchConfigProvider.get().billing.defaultFreeCreditsDollarLimit)) {

      // TODO: prevent setting this limit directly except in this method?
      user.setFreeTierCreditsLimitDollarsOverride(newDollarLimit);
      user = userDao.save(user);

      if (userHasRemainingFreeTierCredits(user)) {
        // may be redundant: enable anyway
        updateFreeTierWorkspacesStatus(user, BillingStatus.ACTIVE);
      }

      userServiceAuditor.fireSetFreeTierDollarLimitOverride(
          user.getUserId(), previousLimitMaybe, newDollarLimit);
      return true;
    }

    return false;
  }

  /**
   * Given a workspace, find the amount of free credits that the workspace creator has left.
   *
   * @param dbWorkspace The workspace for which to find its creator's free credits remaining
   * @return The amount of free credits in USD the workspace creator has left, represented as a
   *     double
   */
  public double getWorkspaceCreatorFreeCreditsRemaining(DbWorkspace dbWorkspace) {
    Double creatorCachedFreeTierUsage = this.getCachedFreeTierUsage(dbWorkspace.getCreator());
    Double creatorFreeTierDollarLimit = this.getUserFreeTierDollarLimit(dbWorkspace.getCreator());
    double creatorFreeCreditsRemaining =
        creatorCachedFreeTierUsage == null
            ? creatorFreeTierDollarLimit
            : creatorFreeTierDollarLimit - creatorCachedFreeTierUsage;
    return Math.max(creatorFreeCreditsRemaining, 0);
  }
}
