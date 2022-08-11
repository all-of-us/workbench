package org.pmiops.workbench.billing;

import static org.pmiops.workbench.db.dao.WorkspaceDao.WorkspaceCostView;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.mail.MessagingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Methods relating to Free Tier credit usage and limits */
@Service
public class FreeTierBillingService {

  public static final int WORKSPACES_BATCH_SIZE = 1000;
  private final BigQueryService bigQueryService;
  private final MailService mailService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final WorkspaceFreeTierUsageService workspaceFreeTierUsageService;

  private static final Logger logger = Logger.getLogger(FreeTierBillingService.class.getName());

  @Autowired
  public FreeTierBillingService(
      BigQueryService bigQueryService,
      MailService mailService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      WorkspaceFreeTierUsageService workspaceFreeTierUsageService) {
    this.bigQueryService = bigQueryService;
    this.mailService = mailService;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userServiceAuditor = userServiceAuditor;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workspaceFreeTierUsageService = workspaceFreeTierUsageService;
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
   * passing thresholds or exceeding limits. RW-6280 - REQUIRES_NEW transactional mode was added to
   * make the call to this method create a new transaction with each set of users. In order to
   * commit the transaction after the call. However, if the user has many workspaces, this method
   * may still timeout. See {@link #updateFreeTierUsageInBatches(List, List)}
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkFreeTierBillingUsageForUsers(Set<DbUser> users) {

    // Current cost in DB
    List<WorkspaceCostView> allCostsInDbForUsers = workspaceDao.getWorkspaceCostViews(users);
    logger.info(String.format("Retrieved %d workspaces from the DB", allCostsInDbForUsers.size()));

    allCostsInDbForUsers =
        findWorkspaceFreeTierUsagesThatWereNotRecentlyUpdated(allCostsInDbForUsers);

    // No need to proceed since there's nothing to update anyway
    if (allCostsInDbForUsers.isEmpty()) {
      return;
    }

    List<WorkspaceCostView> workspacesThatRequireUpdate =
        findWorkspacesThatRequireUpdates(allCostsInDbForUsers);

    final Map<Long, Double> liveCostsInBQ =
        updateFreeTierUsageInBatches(allCostsInDbForUsers, workspacesThatRequireUpdate);

    // Cache cost in DB by creator
    final Map<Long, Double> dbCostByCreator =
        allCostsInDbForUsers.stream()
            .collect(
                Collectors.groupingBy(
                    WorkspaceCostView::getCreatorId,
                    Collectors.summingDouble(
                        v -> Optional.ofNullable(v.getFreeTierCost()).orElse(0.0))));

    // check cost thresholds for the relevant users
    final Map<Long, Long> creatorByWorkspace =
        allCostsInDbForUsers.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceCostView::getWorkspaceId, WorkspaceCostView::getCreatorId));

    // Cache cost in BQ by creator
    final Map<Long, Double> liveCostByCreator =
        liveCostsInBQ.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    e -> creatorByWorkspace.get(e.getKey()),
                    Collectors.summingDouble(Entry::getValue)));

    // Find users with changed costs only
    final Map<Long, DbUser> dbUsersWithChangedCosts =
        findDbUsersWithChangedCosts(dbCostByCreator, liveCostByCreator);

    updateWorkspaceStatusToInactiveForNewlyExpiredUsers(
        users, dbUsersWithChangedCosts, dbCostByCreator, liveCostByCreator);

    sendAlertsForCostThresholds(dbCostByCreator, liveCostByCreator);
  }

  /**
   * Get only the workspaces that their free tier were updated before the configured time. This
   * insures that we don't calculate the costs unnecessarily again if we run the job manually to
   * clear up the backlog
   *
   * @param allCostsInDbForUsers a List of {@link WorkspaceCostView} which contains all info about
   *     workspaces including when they were last updated.
   * @return A filtered list containing the workspaces free tier usage entries that were not last
   *     updated in the last 60 minutes.
   */
  @NotNull
  private List<WorkspaceCostView> findWorkspaceFreeTierUsagesThatWereNotRecentlyUpdated(
      List<WorkspaceCostView> allCostsInDbForUsers) {
    Timestamp minusMinutes =
        Timestamp.valueOf(LocalDateTime.now().minusMinutes(getMinutesBeforeLastFreeTierJob()));

    List<WorkspaceCostView> filteredCostsInDbForUsers =
        allCostsInDbForUsers.stream()
            .filter(
                c ->
                    c.getFreeTierLastUpdated() == null
                        || c.getFreeTierLastUpdated().before(minusMinutes))
            .collect(Collectors.toList());

    logger.info(
        String.format(
            "Retrieved %d workspaces from the DB eligible for updates",
            filteredCostsInDbForUsers.size()));

    return filteredCostsInDbForUsers;
  }

  // TODO: move to DbWorkspace?  RW-5107
  public boolean isFreeTier(final DbWorkspace workspace) {
    return workbenchConfigProvider
        .get()
        .billing
        .freeTierBillingAccountNames()
        .contains(workspace.getBillingAccountName());
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
        || CostComparisonUtils.costsDiffer(
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

  private void updateFreeTierWorkspacesStatus(final DbUser user, final BillingStatus status) {
    workspaceDao.findAllByCreator(user).stream()
        .filter(this::isFreeTier)
        .map(DbWorkspace::getWorkspaceId)
        .forEach(id -> workspaceDao.updateBillingStatus(id, status));
  }

  private void sendAlertsForCostThresholds(
      Map<Long, Double> previousUserCosts, Map<Long, Double> userCosts) {

    final List<Double> costThresholdsInDescOrder =
        workbenchConfigProvider.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    userCosts.forEach(
        (userId, currentCost) -> {
          final double previousCost = previousUserCosts.getOrDefault(userId, 0.0);
          maybeAlertOnCostThresholds(
              userDao.findUserByUserId(userId),
              Math.max(currentCost, previousCost),
              previousCost,
              costThresholdsInDescOrder);
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
    final double previousRemainingBalance = limit - previousCost;

    if (remainingBalance < 0 && previousRemainingBalance > 0) {
      try {
        mailService.alertUserFreeTierExpiration(user);
      } catch (MessagingException e) {
        logger.log(Level.WARNING, "failed to mail free tier expiration email", e);
      }
      return;
    }

    // this shouldn't happen, but it did (RW-4678)
    // alert if it happens again
    if (CostComparisonUtils.compareCosts(currentCost, previousCost) < 0) {
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
      if (CostComparisonUtils.compareCostFractions(currentFraction, threshold) > 0) {
        // only alert if we have not done so previously
        if (CostComparisonUtils.compareCostFractions(previousFraction, threshold) <= 0) {
          try {
            mailService.alertUserFreeTierDollarThreshold(
                user, threshold, currentCost, remainingBalance);
          } catch (final MessagingException e) {
            logger.log(Level.WARNING, "failed to mail threshold email", e);
          }
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }

  private Set<DbUser> getFreeTierActiveWorkspaceCreatorsIn(Set<DbUser> users) {
    return workspaceDao.findCreatorsByBillingStatusAndBillingAccountNameIn(
        BillingStatus.ACTIVE,
        new ArrayList<>(workbenchConfigProvider.get().billing.freeTierBillingAccountNames()),
        users);
  }

  private Map<Long, Double> getFreeTierWorkspaceCostsFromBQ(List<WorkspaceCostView> costInDB) {
    final Map<String, Long> workspaceByProject =
        costInDB.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceCostView::getGoogleProject, WorkspaceCostView::getWorkspaceId));

    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT project.id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` WHERE project.id IS NOT NULL "
                    + " AND project.id IN UNNEST(@projects) "
                    + "GROUP BY project.id ORDER BY cost desc;")
            .addNamedParameter(
                "projects",
                QueryParameterValue.array(
                    workspaceByProject.keySet().toArray(new String[workspaceByProject.size()]),
                    String.class))
            .build();

    final Map<Long, Double> liveCostByWorkspace = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String googleProject = tableRow.get("id").getStringValue();
      if (workspaceByProject.containsKey(googleProject)) {
        liveCostByWorkspace.put(
            workspaceByProject.get(googleProject), tableRow.get("cost").getDoubleValue());
      }
    }

    return liveCostByWorkspace;
  }

  /**
   * Filter the costs further by getting the workspaces that are active, or deleted but their free
   * tier last updated time is before the workspace last updated time. This filtration ensures that
   * BQ will not be queried unnecessarily for the costs of deleted workspaces that we already have
   * their latest costs The method will return the workspace in either of these cases:
   *
   * <ol>
   *   <li>The workspace is active
   *   <li>The workspace is deleted within the past 6 months and any of the following is true.
   *       <ol>
   *         <li>Free Tier Usage last updated time is null. This means that it wasn't calculated
   *             before
   *         <li>Workspace last updated time is null. This means we don't have enough info about the
   *             workspace, so we need to get its cost from BQ
   *         <li>Free Tier Usage time is before the Workspace last updated time (Here the workspace
   *             last updated time will be the time that the workspace was deleted). This means that
   *             the workspace got changed some time after our last calculation, so we need to
   *             recalculate its usage
   *         <li>Free Tier Usage time is after the Workspace last updated time (Here the workspace
   *             last updated time will be the time that the workspace was deleted), but the
   *             difference is smaller than a certain value. This case to account for charges that
   *             may occur after the workspace gets deleted and after the last cron had run.
   *       </ol>
   * </ol>
   *
   * @param allCostsInDbForUsers
   * @return a {@link java.util.List} of the workspaces that require updates only
   */
  @NotNull
  private List<WorkspaceCostView> findWorkspacesThatRequireUpdates(
      List<WorkspaceCostView> allCostsInDbForUsers) {

    List<WorkspaceCostView> workspacesThatRequireUpdate =
        allCostsInDbForUsers.stream()
            .filter(
                c ->
                    (c.getActiveStatus() == 0) // ACTIVE
                        || (c.getActiveStatus() == 1 // DELETED
                            && (c.getWorkspaceLastUpdated() == null
                                || c.getWorkspaceLastUpdated()
                                    .toLocalDateTime()
                                    .isAfter(LocalDateTime.now().minusMonths(6)))
                            && (c.getFreeTierLastUpdated() == null
                                || c.getWorkspaceLastUpdated() == null
                                || c.getFreeTierLastUpdated().before(c.getWorkspaceLastUpdated())
                                || (c.getFreeTierLastUpdated().after(c.getWorkspaceLastUpdated())
                                    && c.getFreeTierLastUpdated().getTime()
                                            - c.getWorkspaceLastUpdated().getTime()
                                        < Duration.ofDays(
                                                workbenchConfigProvider.get()
                                                    .billing
                                                    .numberOfDaysToConsiderForFreeTierUsageUpdate)
                                            .toMillis()))))
            .collect(Collectors.toList());

    logger.info(
        String.format("Workspaces that require update %d", workspacesThatRequireUpdate.size()));

    return workspacesThatRequireUpdate;
  }

  /**
   * Requesting live costs from BQ in batches then updating it in a transactional method. The
   * batching mechanism prevents the job from making huge BQ queries that always timed out,
   * specially in test env where a user may have thousands of workspaces.
   *
   * @param allCostsInDbForUsers List of {@link WorkspaceCostView} containing all workspaces for the
   *     current batch of users
   * @param workspacesThatRequireUpdate List of {@link WorkspaceCostView} containing only the
   *     workspaces that need to be updated
   * @return a Map of all live costs.
   */
  private Map<Long, Double> updateFreeTierUsageInBatches(
      List<WorkspaceCostView> allCostsInDbForUsers,
      List<WorkspaceCostView> workspacesThatRequireUpdate) {
    final Map<Long, Double> liveCostsInBQ = new HashMap<>();

    // Cache that maps a workspace ID to its current cost in the database
    final Map<Long, Double> dbCostByWorkspace =
        allCostsInDbForUsers.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceCostView::getWorkspaceId,
                    v -> Optional.ofNullable(v.getFreeTierCost()).orElse(0.0)));

    // Partition the workspaces in batches, to avoid making BQ queries with thousands of workspaces
    // as in test env, which will eventually time out each time.
    for (List<WorkspaceCostView> workspacesPartition :
        Lists.partition(workspacesThatRequireUpdate, WORKSPACES_BATCH_SIZE)) {
      // Live cost in BQ
      liveCostsInBQ.putAll(getFreeTierWorkspaceCostsFromBQ(workspacesPartition));
      workspaceFreeTierUsageService.updateWorkspaceFreeTierUsageInDB(
          dbCostByWorkspace, liveCostsInBQ);
    }
    return liveCostsInBQ;
  }

  /**
   * Find the users with db costs different from the live costs
   *
   * @param dbCostByCreator Map of userId->dbCost
   * @param liveCostByCreator Map of userId->liveCost
   * @return a {@link Map} of user Ids to their DbUsers
   */
  @NotNull
  private Map<Long, DbUser> findDbUsersWithChangedCosts(
      Map<Long, Double> dbCostByCreator, Map<Long, Double> liveCostByCreator) {
    final Set<Long> usersWithChangedCosts =
        liveCostByCreator.keySet().stream()
            .filter(
                u ->
                    CostComparisonUtils.compareCosts(
                            dbCostByCreator.get(u), liveCostByCreator.get(u))
                        != 0)
            .collect(Collectors.toSet());

    logger.info(String.format("Found %d users with changed costs.", usersWithChangedCosts.size()));

    final Map<Long, DbUser> dbUsersWithChangedCosts = new HashMap<>();
    userDao
        .findAllById(usersWithChangedCosts)
        .forEach(user -> dbUsersWithChangedCosts.put(user.getUserId(), user));
    return dbUsersWithChangedCosts;
  }

  /**
   * Get the list of newly expired users (who exceeded their free tier limit) and mark all their
   * workspaces as inactive
   *
   * @param allUsers set of all users to filter them whether they have active free tier workspace
   * @param dbUsersWithChangedCosts The map of users with changed costs, to calculate which of them
   *     exceeded their free tier limit
   * @param costByCreator Map of userId->dbCost
   * @param liveCostByCreator Map of userId->liveCost
   */
  private void updateWorkspaceStatusToInactiveForNewlyExpiredUsers(
      final Set<DbUser> allUsers,
      Map<Long, DbUser> dbUsersWithChangedCosts,
      Map<Long, Double> costByCreator,
      Map<Long, Double> liveCostByCreator) {

    Set<DbUser> freeTierUsers = getFreeTierActiveWorkspaceCreatorsIn(allUsers);

    // Find users who exceeded their free tier limit
    // Here costs in liveCostByCreator could be outdated because we're filtering on active  or
    // recently deleted workspaces
    // in previous steps. However, costByCreator will contain the up-to-date costs for all the other
    // workspaces.
    // This is why Math.max is used
    final Set<DbUser> expiredUsers =
        dbUsersWithChangedCosts.entrySet().stream()
            .filter(
                e ->
                    costAboveLimit(
                        e.getValue(),
                        Math.max(costByCreator.get(e.getKey()), liveCostByCreator.get(e.getKey()))))
            .map(Entry::getValue)
            .collect(Collectors.toSet());

    final Set<DbUser> newlyExpiredFreeTierUsers = Sets.intersection(expiredUsers, freeTierUsers);

    logger.info(
        String.format(
            "Found %d users exceeding their free tier limit, out of which, %d are new",
            expiredUsers.size(), newlyExpiredFreeTierUsers.size()));

    for (final DbUser user : newlyExpiredFreeTierUsers) {
      updateFreeTierWorkspacesStatus(
          user, BillingStatus.INACTIVE); // Not optimal, should run 1 update statement
    }
  }

  private boolean costAboveLimit(final DbUser user, final double currentCost) {
    return CostComparisonUtils.compareCosts(currentCost, getUserFreeTierDollarLimit(user)) > 0;
  }

  private Integer getMinutesBeforeLastFreeTierJob() {
    return Optional.ofNullable(workbenchConfigProvider.get().billing.minutesBeforeLastFreeTierJob)
        .orElse(120);
  }
}
