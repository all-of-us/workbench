package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import javax.mail.MessagingException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.pmiops.workbench.db.dao.WorkspaceDao.*;

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
  private final WorkspaceFreeTierUsageService workspaceFreeTierUsageService;

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
   * Check whether usersPartition have incurred sufficient cost in their workspaces to trigger alerts due to
   * passing thresholds or exceeding limits
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void checkFreeTierBillingUsageForUsers(Set<DbUser> usersPartition) {

    // Current cost in DB
    List<WorkspaceCostView> allCostsInDbForUsers = workspaceDao.getWorkspaceCostViews(usersPartition);
    Timestamp minusMinutes = Timestamp.valueOf(
            LocalDateTime.now().minusMinutes(getMinutesBeforeLastFreeTierJob()));

    logger.info(String.format("Retrieved %d workspaces from the DB", allCostsInDbForUsers.size()));

    allCostsInDbForUsers =
        allCostsInDbForUsers.stream()
            .filter(
                c ->
                    c.getFreeTierLastUpdated() == null
                            || c.getFreeTierLastUpdated().before(minusMinutes)
            )
            .collect(Collectors.toList());

    List<WorkspaceCostView> workspacesThatRequireUpdate =
        allCostsInDbForUsers.stream()
            .filter(
                c ->
                    (c.getActiveStatus() == 0)
                        || (c.getActiveStatus() == 1
                            && (c.getFreeTierLastUpdated() == null
                                || c.getWorkspaceLastUpdated() == null
                                || c.getFreeTierLastUpdated().before(c.getWorkspaceLastUpdated()))))
            .collect(Collectors.toList());

    logger.info(String.format("workspacesThatRequireUpdate %d", workspacesThatRequireUpdate.size()));
    logger.info(String.format("Retrieved %d workspaces from the DB eligible for updates", allCostsInDbForUsers.size()));

    // No need to call BigQuery since there's nothing to update anyway
    if(allCostsInDbForUsers.isEmpty()) return;

    // Live cost in BQ
    final Map<Long, Double> liveCostsInBQ = getFreeTierWorkspaceCostsFromBQ(workspacesThatRequireUpdate);

    logger.info(String.format("Retrieved %d workspaces from BQ", liveCostsInBQ.size()));

    updateWorkspaceFreeTierUsageInDB(allCostsInDbForUsers, liveCostsInBQ);

    // Cache cost in DB by creator
    final Map<Long, Double> dbCostByCreator =
            allCostsInDbForUsers.stream()
                    .collect(
                            Collectors.groupingBy(
                                    WorkspaceCostView::getCreatorId,
                                    Collectors.summingDouble(
                                            v -> Optional.ofNullable(v.getFreeTierCost()).orElse(0.0))));

    // check cost thresholds for the relevant usersPartition
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
    final Set<Long> usersWithChangedCosts =
            liveCostByCreator.keySet().stream()
                    .filter(u -> compareCosts(dbCostByCreator.get(u), liveCostByCreator.get(u)) != 0)
                    .collect(Collectors.toSet());

    logger.info(String.format("Found %d users with changed costs.", usersWithChangedCosts.size()));

    final Map<Long, DbUser> dbUsersWithChangedCosts = new HashMap<>();
    userDao
            .findAllById(usersWithChangedCosts)
            .forEach(user -> dbUsersWithChangedCosts.put(user.getUserId(), user));

    updateWorkspaceStatusToInactiveForNewlyExpiredUsers(usersPartition, dbUsersWithChangedCosts, dbCostByCreator, liveCostByCreator);

    sendAlertsForCostThresholds(dbCostByCreator, liveCostByCreator);

  }

  private void updateWorkspaceStatusToInactiveForNewlyExpiredUsers(final Set<DbUser> allUsers,
                                                                   Map<Long, DbUser> dbUsersWithChangedCosts, Map<Long, Double> costByCreator, Map<Long, Double> liveCostByCreator) {

    Set<DbUser> freeTierUsers = getFreeTierActiveWorkspaceCreatorsIn(allUsers);

    // Find users who exceeded their free tier limit
    final Set<DbUser> expiredUsers =
            dbUsersWithChangedCosts.entrySet().stream()
                    .filter(e -> costAboveLimit(e.getValue(), Math.max(costByCreator.get(e.getKey()),
                            liveCostByCreator.get(e.getKey()))))
                    .map(Entry::getValue)
                    .collect(Collectors.toSet());


    final Set<DbUser> newlyExpiredFreeTierUsers = Sets.intersection(expiredUsers, freeTierUsers);

    logger.info(
            String.format(
                    "Found %d users exceeding their free tier limit, out of which, %d are new",
                    expiredUsers.size(), newlyExpiredFreeTierUsers.size()));

    for (final DbUser user : newlyExpiredFreeTierUsers) {
      updateFreeTierWorkspacesStatus(user, BillingStatus.INACTIVE);  //Not optimal, should run 1 update statement
    }
  }

  private void updateWorkspaceFreeTierUsageInDB(
          List<WorkspaceCostView> dbCost, Map<Long, Double> liveCostByWorkspace) {
    final Map<Long, Double> dbCostByWorkspace =
            dbCost.stream()
                    .collect(
                            Collectors.toMap(
                                    WorkspaceCostView::getWorkspaceId,
                                    v -> Optional.ofNullable(v.getFreeTierCost()).orElse(0.0)));

    final List<Long> workspacesIdsToUpdate =
            liveCostByWorkspace.keySet()
                    .stream()
                    .filter(currentId ->
                            compareCosts(dbCostByWorkspace.get(currentId), liveCostByWorkspace.get(currentId)) != 0)
                    .collect(
                            Collectors.toList());

    final Iterable<DbWorkspace> workspaceList = workspaceDao.findAllById(workspacesIdsToUpdate);
    final Iterable<DbWorkspaceFreeTierUsage> workspaceFreeTierUsages = workspaceFreeTierUsageDao.findAllByWorkspaceIn(workspaceList);

    // Prepare cache of workspace ID to the free tier use entity
    Map<Long, DbWorkspaceFreeTierUsage> workspaceIdToFreeTierUsage =
            StreamSupport
                    .stream(workspaceFreeTierUsages.spliterator(), false)
                    .collect(
                            Collectors
                                    .toMap(wftu -> wftu.getWorkspace().getWorkspaceId(), Function.identity()));

    workspaceList.forEach(
            w -> workspaceFreeTierUsageService.updateCost(workspaceIdToFreeTierUsage, w, liveCostByWorkspace.get(w.getWorkspaceId()))); // TODO updateCost queries for each workspace, can be optimized by getting all needed workspaces in one query

    logger.info(
            String.format(
                    "found changed cost information for %d/%d workspaces",
                    workspacesIdsToUpdate.size(), dbCostByWorkspace.size()));
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
  public boolean isFreeTier(final DbWorkspace workspace) {
    return workbenchConfigProvider
            .get()
            .billing
            .freeTierBillingAccountNames()
            .contains(workspace.getBillingAccountName());
  }

  private void updateFreeTierWorkspacesStatus(final DbUser user, final BillingStatus status) {
    workspaceDao.findAllByCreator(user).stream()
            .filter(this::isFreeTier)
            .map(DbWorkspace::getWorkspaceId)
            .forEach(id -> workspaceDao.updateBillingStatus(id, status));
  }

  private void sendAlertsForCostThresholds(Map<Long, Double> previousUserCosts,
          Map<Long, Double> userCosts) {

    final List<Double> costThresholdsInDescOrder =
            workbenchConfigProvider.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    userCosts.forEach(
            (userId, currentCost) -> {
              final double previousCost = previousUserCosts.getOrDefault(userId, 0.0);
              maybeAlertOnCostThresholds(userDao.findUserByUserId(userId), Math.max(currentCost, previousCost), previousCost, costThresholdsInDescOrder);
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
            logger.log(Level.WARNING, "failed to mail threshold email", e);
          }
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }

  // Helper to identify candidate users with workspaces that may need deactivation, if those users'
  // initial credits have expired.
  private Set<DbUser> getFreeTierActiveWorkspaceCreators() {
    return workspaceDao.findAllCreatorsByBillingStatusAndBillingAccountNameIn(
            BillingStatus.ACTIVE,
            new ArrayList<String>(workbenchConfigProvider.get().billing.freeTierBillingAccountNames()));
  }

  private Set<DbUser> getFreeTierActiveWorkspaceCreatorsIn(Set<DbUser> users) {
    return workspaceDao.findCreatorsByBillingStatusAndBillingAccountNameIn(
            BillingStatus.ACTIVE,
            new ArrayList<String>(workbenchConfigProvider.get().billing.freeTierBillingAccountNames()),
            users);
  }

  private Map<Long, Double>  getFreeTierWorkspaceCostsFromBQ(List<WorkspaceCostView> costInDB) {
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
                    .addNamedParameter("projects",
                            QueryParameterValue.array(workspaceByProject.keySet().toArray
                                    (new String[workspaceByProject.size()]), String.class))
                    .build();

    final Map<Long, Double> liveCostByWorkspace = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String googleProject = tableRow.get("id").getStringValue();
      if (workspaceByProject.containsKey(googleProject)) {
        liveCostByWorkspace.put(
                workspaceByProject.get(googleProject),
                tableRow.get("cost").getDoubleValue());
      }
    }

    return liveCostByWorkspace;

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

  private Integer getMinutesBeforeLastFreeTierJob() {
    return Optional.ofNullable(workbenchConfigProvider.get().billing.minutesBeforeLastFreeTierJob).orElse(120);
  }



}
