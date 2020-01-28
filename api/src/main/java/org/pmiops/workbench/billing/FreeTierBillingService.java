package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Sets;
import java.sql.Timestamp;
import java.time.*;
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
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
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

  private final Clock clock;

  private static final Logger logger = Logger.getLogger(FreeTierBillingService.class.getName());

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      MailService mailService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Clock clock) {
    this.bigQueryService = bigQueryService;
    this.mailService = mailService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.clock = clock;
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

    final Set<DbUser> costExpiredUsers =
        userCosts.entrySet().stream()
            .filter(e -> expiredByCost(e.getKey(), e.getValue()))
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    final Set<DbUser> timeExpiredUsers =
        userDao.findByFirstRegistrationCompletionTimeNotNull().stream()
            .filter(this::expiredByTime)
            .collect(Collectors.toSet());

    final Set<DbUser> currentExpiredUsers = Sets.union(costExpiredUsers, timeExpiredUsers);

    final Set<DbUser> newlyExpiredUsers =
        Sets.difference(currentExpiredUsers, previouslyExpiredUsers);
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
        Sets.difference(usersWithNonNullRegistration, currentExpiredUsers);

    sendAlertsForCostThresholds(usersToThresholdCheck, previousUserCosts, userCosts);
    sendAlertsForTimeThresholds(usersToThresholdCheck, userCosts);
  }

  private boolean expiredByCost(final DbUser user, final double currentCost) {
    return currentCost > getUserFreeTierDollarLimit(user);
  }

  private Optional<Instant> getUserFreeCreditExpirationTime(final DbUser user) {
    return Optional.ofNullable(user.getFirstRegistrationCompletionTime())
        .map(
            registrationTime ->
                registrationTime.toInstant().plus(Duration.ofDays(getUserFreeTierDaysLimit(user))));
  }

  private boolean expiredByTime(final DbUser user) {
    return getUserFreeCreditExpirationTime(user)
        .map(time -> clock.instant().isAfter(time))
        .orElse(false);
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

  private void sendAlertsForTimeThresholds(
      Set<DbUser> usersToCheck, Map<DbUser, Double> userCosts) {
    final List<Double> timeThresholdsInDescOrder =
        workbenchConfigProvider.get().billing.freeTierTimeAlertThresholds;
    timeThresholdsInDescOrder.sort(Comparator.reverseOrder());

    for (final DbUser user : usersToCheck) {
      final double currentCost = userCosts.getOrDefault(user, 0.0);
      final double remainingDollarBalance = getUserFreeTierDollarLimit(user) - currentCost;
      maybeAlertOnTimeThresholds(user, remainingDollarBalance, timeThresholdsInDescOrder);

      // save current check time
      user.setLastFreeTierCreditsTimeCheck(Timestamp.from(clock.instant()));
      userDao.save(user);
    }
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

    final double currentFraction = currentCost / limit;
    final double previousFraction = previousCost / limit;

    for (final double threshold : thresholdsInDescOrder) {
      if (currentFraction > threshold) {
        // only alert if we have not done so previously
        if (previousFraction <= threshold) {
          try {
            mailService.alertUserFreeTierDollarThreshold(
                user,
                threshold,
                currentCost,
                remainingBalance,
                getUserFreeCreditExpirationTime(user));
          } catch (final MessagingException e) {
            logger.log(Level.WARNING, e.getMessage());
          }
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }

  /**
   * Has this user passed a time threshold between this check and the previous run?
   *
   * <p>Compare this user's free credits timespan with that of the previous run, and trigger an
   * alert if this is the run which pushed it over a threshold.
   *
   * @param user The user to check
   * @param remainingDollarBalance The remaining dollar balance to this user, for reporting purposes
   * @param thresholdsInDescOrder the time alerting thresholds, in descending order
   */
  private void maybeAlertOnTimeThresholds(
      DbUser user, double remainingDollarBalance, List<Double> thresholdsInDescOrder) {
    final Instant userFreeCreditStartTime = user.getFirstRegistrationCompletionTime().toInstant();

    final Instant previousCheckTime =
        Optional.ofNullable(user.getLastFreeTierCreditsTimeCheck())
            .map(Timestamp::toInstant)
            .orElse(userFreeCreditStartTime);

    final Duration userFreeCreditDays = Duration.ofDays(getUserFreeTierDaysLimit(user));
    final Duration currentTimeElapsed = Duration.between(userFreeCreditStartTime, clock.instant());
    final Duration previousTimeElapsed =
        Duration.between(userFreeCreditStartTime, previousCheckTime);

    // can't use toDays() here because it truncates and we need sub-day resolution
    final double currentFraction =
        (double) currentTimeElapsed.toMillis() / userFreeCreditDays.toMillis();
    final double previousFraction =
        (double) previousTimeElapsed.toMillis() / userFreeCreditDays.toMillis();

    final Instant userFreeCreditExpirationTime = userFreeCreditStartTime.plus(userFreeCreditDays);
    final Duration timeRemaining = Duration.between(clock.instant(), userFreeCreditExpirationTime);
    final LocalDate expirationDate =
        userFreeCreditExpirationTime.atZone(ZoneId.systemDefault()).toLocalDate();

    for (final double threshold : thresholdsInDescOrder) {
      if (currentFraction > threshold) {
        // only alert if we have not done so previously
        if (previousFraction <= threshold) {
          try {
            mailService.alertUserFreeTierTimeThreshold(
                user, timeRemaining.toDays(), expirationDate, remainingDollarBalance);
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
   * @param user the user as represented in our database
   * @return the total USD amount spent in workspaces created by this user, represented as a double
   */
  public Double getUserCachedFreeTierUsage(DbUser user) {
    return workspaceFreeTierUsageDao.totalCostByUser(user);
  }

  /**
   * Retrieve the Free Tier dollar limit actually applicable to this user: this user's override if
   * present, the environment's default if not
   *
   * @param user the user as represented in our database
   * @return the US dollar amount, represented as a double
   */
  public double getUserFreeTierDollarLimit(DbUser user) {
    final Double override = user.getFreeTierCreditsLimitDollarsOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDollarLimit;
  }

  /**
   * Retrieve the Free Tier time limit actually applicable to this user: this user's override if
   * present, the environment's default if not
   *
   * @param user the user as represented in our database
   * @return the number of days after registration the user is permitted to access the Free Tier
   */
  public short getUserFreeTierDaysLimit(DbUser user) {
    final Short override = user.getFreeTierCreditsLimitDaysOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDaysLimit;
  }
}
