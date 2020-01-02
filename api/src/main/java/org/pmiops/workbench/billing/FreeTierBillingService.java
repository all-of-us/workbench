package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Sets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
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

  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      NotificationService notificationService,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Check whether users have incurred sufficient cost or time in their workspaces to trigger alerts
   * due to passing thresholds or exceeding limits
   */
  public void checkFreeTierBillingUsage() {
    // freeze this value, for later consistency
    final Instant currentCheckTime = Instant.now();

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
    final Set<DbUser> currentExpiredUsers =
        Sets.union(getCostExpiredUsers(userCosts), getTimeExpiredUsers(currentCheckTime));
    processNewlyExpiredUsers(Sets.difference(currentExpiredUsers, previouslyExpiredUsers));

    // check for intermediate thresholds and alert, possibly for both cost and time

    // by cost

    final List<Double> costThresholdsInDescOrder =
        workbenchConfigProvider.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    userCosts.forEach(
        (user, currentCost) -> {
          if (!currentExpiredUsers.contains(user)) {
            final double previousCost = previousUserCosts.getOrDefault(user, 0.0);
            maybeAlertOnCostThresholds(user, currentCost, previousCost, costThresholdsInDescOrder);
          }
        });

    // by time

    final List<Double> timeThresholdsInDescOrder =
        workbenchConfigProvider.get().billing.freeTierTimeAlertThresholds;
    timeThresholdsInDescOrder.sort(Comparator.reverseOrder());

    for (final DbUser user : userDao.findByFirstRegistrationCompletionTimeNotNull()) {
      if (!currentExpiredUsers.contains(user)) {
        final double currentCost = userCosts.getOrDefault(user, 0.0);
        final double remainingDollarBalance = getUserFreeTierDollarLimit(user) - currentCost;
        maybeAlertOnTimeThresholds(
            user, remainingDollarBalance, currentCheckTime, timeThresholdsInDescOrder);

        // save current check time
        user.setLastFreeTierCreditsTimeCheck(Timestamp.from(currentCheckTime));
        userDao.save(user);
      }
    }
  }

  @NotNull
  private Set<DbUser> getTimeExpiredUsers(Instant expirationCheckTime) {
    return userDao.findByFirstRegistrationCompletionTimeNotNull().stream()
        .filter(
            u -> {
              final Instant userFreeCreditStartTime =
                  u.getFirstRegistrationCompletionTime().toInstant();
              final Duration userFreeCreditDays = Duration.ofDays(getUserFreeTierDaysLimit(u));
              final Instant userFreeCreditExpirationTime =
                  userFreeCreditStartTime.plus(userFreeCreditDays);
              return expirationCheckTime.isAfter(userFreeCreditExpirationTime);
            })
        .collect(Collectors.toSet());
  }

  @NotNull
  private Set<DbUser> getCostExpiredUsers(Map<DbUser, Double> userCosts) {
    return userCosts.entrySet().stream()
        .filter(
            entry -> {
              final DbUser u = entry.getKey();
              final double currentCost = entry.getValue();
              return currentCost > getUserFreeTierDollarLimit(u);
            })
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }

  private void processNewlyExpiredUsers(Set<DbUser> newlyExpiredUsers) {
    for (final DbUser user : newlyExpiredUsers) {
      notificationService.alertUserFreeTierExpiration(user);
      final Set<DbWorkspace> toDeactivate = workspaceDao.findAllByCreator(user);
      for (final DbWorkspace workspace : toDeactivate) {
        workspaceDao.updateBillingStatus(workspace.getWorkspaceId(), BillingStatus.INACTIVE);
      }
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
          notificationService.alertUserFreeTierDollarThreshold(
              user, threshold, currentCost, remainingBalance);
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
   * @param currentCheckTime a fixed time common to all checks of this run, for comparison purposes
   * @param thresholdsInDescOrder the time alerting thresholds, in descending order
   */
  private void maybeAlertOnTimeThresholds(
      DbUser user,
      double remainingDollarBalance,
      Instant currentCheckTime,
      List<Double> thresholdsInDescOrder) {
    final Instant userFreeCreditStartTime = user.getFirstRegistrationCompletionTime().toInstant();

    final Instant previousCheckTime =
        Optional.ofNullable(user.getLastFreeTierCreditsTimeCheck())
            .map(Timestamp::toInstant)
            .orElse(userFreeCreditStartTime);

    final Duration userFreeCreditDays = Duration.ofDays(getUserFreeTierDaysLimit(user));
    final Duration currentTimeElapsed = Duration.between(userFreeCreditStartTime, currentCheckTime);
    final Duration previousTimeElapsed =
        Duration.between(userFreeCreditStartTime, previousCheckTime);

    // can't use toDays() here because it truncates and we need sub-day resolution
    final double currentFraction =
        (double) currentTimeElapsed.toMillis() / userFreeCreditDays.toMillis();
    final double previousFraction =
        (double) previousTimeElapsed.toMillis() / userFreeCreditDays.toMillis();

    final Instant userFreeCreditExpirationTime = userFreeCreditStartTime.plus(userFreeCreditDays);
    final Duration timeRemaining = Duration.between(currentCheckTime, userFreeCreditExpirationTime);

    for (final double threshold : thresholdsInDescOrder) {
      if (currentFraction > threshold) {
        // only alert if we have not done so previously
        if (previousFraction <= threshold) {
          notificationService.alertUserFreeTierTimeThreshold(
              user,
              timeRemaining.toDays(),
              userFreeCreditExpirationTime.atZone(ZoneId.systemDefault()).toLocalDate(),
              remainingDollarBalance);
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }

  // we set Workspaces to EXPIRED when their creators have exceeded their free credits
  // so we can retrieve these users by querying for expired workspaces
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

  // Retrieve the user's total free tier usage from the DB by summing across Workspaces.
  // This is not live BigQuery data: it is only as recent as the last
  // checkFreeTierBillingUsage cron job, recorded as last_update_time in the DB.
  public Double getUserCachedFreeTierUsage(DbUser user) {
    return workspaceFreeTierUsageDao.totalCostByUser(user);
  }

  public double getUserFreeTierDollarLimit(DbUser user) {
    final Double override = user.getFreeTierCreditsLimitDollarsOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDollarLimit;
  }

  public short getUserFreeTierDaysLimit(DbUser user) {
    final Short override = user.getFreeTierCreditsLimitDaysOverride();
    if (override != null) {
      return override;
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsDaysLimit;
  }
}
