package org.pmiops.workbench.initialcredits;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.pmiops.workbench.db.dao.WorkspaceDao.WorkspaceCostView;
import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.BillingUtils;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Methods relating to initial credits usage and limits */
@Service
public class InitialCreditsService {
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final InstitutionService institutionService;
  private final LeonardoApiClient leonardoApiClient;
  private final MailService mailService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final TaskQueueService taskQueueService;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private final WorkspaceInitialCreditUsageService workspaceInitialCreditUsageService;
  private final WorkspaceMapper workspaceMapper;

  private static final Logger logger = LoggerFactory.getLogger(InitialCreditsService.class);

  @Autowired
  public InitialCreditsService(
      Clock clock,
      FireCloudService fireCloudService,
      InstitutionService institutionService,
      LeonardoApiClient leonardoApiClient,
      MailService mailService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      WorkspaceDao workspaceDao,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      WorkspaceInitialCreditUsageService workspaceInitialCreditUsageService,
      WorkspaceMapper workspaceMapper) {
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.institutionService = institutionService;
    this.leonardoApiClient = leonardoApiClient;
    this.mailService = mailService;
    this.taskQueueService = taskQueueService;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.workspaceInitialCreditUsageService = workspaceInitialCreditUsageService;
    this.workspaceMapper = workspaceMapper;
  }

  public double getWorkspaceInitialCreditsUsage(DbWorkspace dbWorkspace) {
    DbWorkspaceFreeTierUsage usage = workspaceFreeTierUsageDao.findOneByWorkspace(dbWorkspace);
    if (usage == null) {
      return 0;
    }
    return usage.getCost();
  }

  /**
   * Check whether users have incurred sufficient cost in their workspaces to trigger alerts due to
   * passing thresholds or exceeding limits.
   */
  public void checkInitialCreditsUsageForUsers(
      Set<DbUser> users, final Map<String, Double> liveCostsInBQ) {
    String userIdsAsString =
        users.stream()
            .map(DbUser::getUserId)
            .sorted()
            .map(l -> Long.toString(l))
            .collect(Collectors.joining(","));
    logger.info(String.format("Checking billing usage for user IDs: %s ", userIdsAsString));
    // Current cost in DB for those workspaces which have not been "recently" updated, as defined by
    // a config value.  This guards against excessive updating of workspaces.
    List<WorkspaceCostView> dbCostsForNotRecentlyUpdatedWorkspaces =
        getNotRecentlyUpdatedDbCostsForUsers(users);

    final Map<String, Long> workspaceByProject =
        getWorkspaceByProjectCache(dbCostsForNotRecentlyUpdatedWorkspaces);
    if (workspaceByProject.isEmpty()) {
      logger.info("No workspaces require updates");
      return;
    }
    updateInitialCreditsUsageInDb(
        dbCostsForNotRecentlyUpdatedWorkspaces, liveCostsInBQ, workspaceByProject);

    // Cache cost in DB by creator
    final Map<Long, Double> dbCostByCreator =
        getDbCostByCreatorCache(dbCostsForNotRecentlyUpdatedWorkspaces);
    // check cost thresholds for the relevant users
    final Map<Long, Long> creatorByWorkspace =
        getCreatorByWorkspaceCache(dbCostsForNotRecentlyUpdatedWorkspaces);
    // Cache cost in BQ by creator
    final Map<Long, Double> liveCostByCreator =
        getLiveCostByCreatorCache(liveCostsInBQ, workspaceByProject, creatorByWorkspace);

    Set<DbUser> filteredUsers = filterUsersHigherThanTheLowestThreshold(users, liveCostByCreator);
    if (filteredUsers.isEmpty()) {
      return;
    }

    taskQueueService.pushInitialCreditsExhaustionTask(
        filteredUsers.stream().map(DbUser::getUserId).toList(), dbCostByCreator, liveCostByCreator);
  }

  /**
   * Filter the users to get only the users that have costs higher than the lowest threshold. This
   * means we'll be pushing cloud tasks for these users to notify them about their costs but the
   * decision to notify or not is left to the handleInitialCreditsExhaustionBatch cloud task.
   * Another way to think about this is to have the logic to decide whether to include the user to
   * be notified here. This is done to avoid pushing cloud tasks for users that have costs lower
   * than the lowest threshold.
   *
   * @param users the users to filter
   * @param liveCostByCreator the live cost by creator
   * @return the users that have costs higher than the lowest threshold
   */
  private Set<DbUser> filterUsersHigherThanTheLowestThreshold(
      Set<DbUser> users, final Map<Long, Double> liveCostByCreator) {
    return workbenchConfigProvider.get().billing.initialCreditsCostAlertThresholds.stream()
        .min(Comparator.naturalOrder())
        .map(
            lowestThreshold ->
                users.stream()
                    .filter(
                        user -> {
                          final double limit = getUserInitialCreditsLimit(user);
                          final double userLiveCost =
                              Optional.ofNullable(liveCostByCreator.get(user.getUserId()))
                                  .orElse(0.0);
                          return userLiveCost / limit >= lowestThreshold;
                        })
                    .collect(Collectors.toSet()))
        .orElse(Collections.emptySet());
  }

  /**
   * Retrieve the user's total initial credits usage from the DB by summing across the Workspaces
   * they have created. This is NOT live BigQuery data: it is only as recent as the last
   * checkInitialCreditsUsage cron job, recorded as last_update_time in the DB.
   *
   * <p>Note: return value may be null, to enable direct assignment to the nullable Profile field
   *
   * @param user the user as represented in our database
   * @return the total USD amount spent in workspaces created by this user, represented as a double
   */
  @Nullable
  public Double getCachedInitialCreditsUsage(DbUser user) {
    return workspaceFreeTierUsageDao.totalCostByUser(user);
  }

  /**
   * Does this user have remaining initial credits? Compare the user-specific initial credits limit
   * (may be the system default) to the amount they have used.
   *
   * @param user the user as represented in our database
   * @return whether the user has remaining credits
   */
  public boolean userHasRemainingInitialCredits(DbUser user) {
    final double usage = Optional.ofNullable(getCachedInitialCreditsUsage(user)).orElse(0.0);
    return !costAboveLimit(user, usage);
  }

  /**
   * Retrieve the Initial Credits limit actually applicable to this user: this user's override if
   * present, the environment's default if not
   *
   * @param user the user as represented in our database
   * @return the US dollar amount, represented as a double
   */
  public double getUserInitialCreditsLimit(DbUser user) {
    return CostComparisonUtils.getUserInitialCreditsLimit(
        user, workbenchConfigProvider.get().billing.defaultInitialCreditsDollarLimit);
  }

  /**
   * Set an Initial Credits limit override value for this user, but only if the value to set differs
   * from the system default or the user has an existing override. If the user has no override and
   * the value to set it equal to the system default, retain the system default so this user's quota
   * continues to track it.
   *
   * <p>If this is greater than the user's total cost, set their workspaces to active. Note:
   * lowering the limit below total cost will NOT set the workspaces to inactive.
   * checkInitialCreditsUsage() will do this as part of the next cron run.
   *
   * @param user the user as represented in our database
   * @param newDollarLimit the US dollar amount, represented as a double
   * @return whether an override was set
   */
  public boolean maybeSetDollarLimitOverride(DbUser user, double newDollarLimit) {
    final Double previousLimitMaybe = user.getInitialCreditsLimitOverride();

    if (!areUserCreditsExpired(user)
        && (previousLimitMaybe != null
            || CostComparisonUtils.costsDiffer(
                newDollarLimit,
                workbenchConfigProvider.get().billing.defaultInitialCreditsDollarLimit))) {

      // TODO: prevent setting this limit directly except in this method?
      user = userDao.save(user.setInitialCreditsLimitOverride(newDollarLimit));

      if (userHasRemainingInitialCredits(user)) {
        // may be redundant: clear exhaustion anyway
        updateInitialCreditsExhaustion(user, false);
      }

      userServiceAuditor.fireSetInitialCreditsOverride(
          user.getUserId(), previousLimitMaybe, newDollarLimit);
      return true;
    }

    return false;
  }

  /**
   * Given a workspace, find the amount of initial credits that the workspace creator has left.
   *
   * @param dbWorkspace The workspace for which to find its creator's initial credits remaining
   * @return The amount of initial credits in USD the workspace creator has left, represented as a
   *     double
   */
  public double getWorkspaceCreatorInitialCreditsRemaining(DbWorkspace dbWorkspace) {
    Double creatorCachedUsage = this.getCachedInitialCreditsUsage(dbWorkspace.getCreator());
    Double creatorLimit = this.getUserInitialCreditsLimit(dbWorkspace.getCreator());
    double creatorCreditsRemaining =
        creatorCachedUsage == null ? creatorLimit : creatorLimit - creatorCachedUsage;
    return Math.max(creatorCreditsRemaining, 0);
  }

  /**
   * For each of the users corresponding to the given user IDs, check if their initial credits have
   * expired, or will handle soon, and handle accordingly.
   *
   * @param userIdsList - The list of user IDs to check for initial credits expiration
   */
  public void checkCreditsExpirationForUserIDs(List<Long> userIdsList) {
    if (userIdsList != null && !userIdsList.isEmpty()) {
      userDao.findAllById(userIdsList).forEach(this::checkExpiration);
    }
  }

  /**
   * For the given user, check when the user's initial credits will expire, if relevant.
   *
   * @param user - The user whose initial credits expiration time is being checked
   * @return The expiration time of the user's initial credits, if they have a
   *     UserInitialCreditsExpiration record, and they have not been bypassed personally or
   *     institutionally.
   */
  public Optional<Timestamp> getCreditsExpiration(DbUser user) {
    return workbenchConfigProvider.get().featureFlags.enableInitialCreditsExpiration
        ? Optional.ofNullable(user.getUserInitialCreditsExpiration())
            .filter(exp -> !exp.isBypassed()) // If the expiration is bypassed, return empty.
            .filter(exp -> !institutionService.shouldBypassForCreditsExpiration(user))
            .map(DbUserInitialCreditsExpiration::getExpirationTime)
        : Optional.empty();
  }

  /**
   * For the given user, check when the user's initial credits were extended, if relevant.
   *
   * @param user - The user whose initial credits extension time is being checked
   * @return The extension time of the user's initial credits, if they have a
   *     UserInitialCreditsExpiration record, and they have not been bypassed personally or
   *     institutionally.
   */
  public Optional<Timestamp> getCreditsExtension(DbUser user) {
    return Optional.ofNullable(user.getUserInitialCreditsExpiration())
        .filter(exp -> !exp.isBypassed()) // If the expiration is bypassed, return empty.
        .filter(exp -> !institutionService.shouldBypassForCreditsExpiration(user))
        .map(DbUserInitialCreditsExpiration::getExtensionTime);
  }

  /**
   * Check if the user's initial credits have expired.
   *
   * @param user - The user whose initial credits expiration time is being checked.
   * @return True if the user's initial credits have expired, false otherwise.
   */
  public boolean areUserCreditsExpired(DbUser user) {
    return getCreditsExpiration(user)
        .map(expirationTime -> !expirationTime.after(clockNow()))
        .orElse(false);
  }

  // Returns true if the user's credits are expiring within the initialCreditsExpirationWarningDays.
  private boolean areCreditsExpiringSoon(DbUser user) {
    long initialCreditsExpirationWarningDays =
        workbenchConfigProvider.get().billing.initialCreditsExpirationWarningDays;
    return getCreditsExpiration(user)
        .map(
            expirationTime ->
                clockNow()
                    .after(
                        new Timestamp(
                            expirationTime.getTime()
                                - TimeUnit.DAYS.toMillis(initialCreditsExpirationWarningDays))))
        .orElse(false);
  }

  public DbUserInitialCreditsExpiration createInitialCreditsExpiration(DbUser user) {
    long initialCreditsValidityPeriodDays =
        workbenchConfigProvider.get().billing.initialCreditsValidityPeriodDays;
    Timestamp now = clockNow();
    Timestamp expirationTime =
        new Timestamp(now.getTime() + TimeUnit.DAYS.toMillis(initialCreditsValidityPeriodDays));
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(now)
            .setExpirationTime(expirationTime)
            .setUser(user);
    user.setUserInitialCreditsExpiration(userInitialCreditsExpiration);
    return userInitialCreditsExpiration;
  }

  public void setInitialCreditsExpirationBypassed(DbUser user, boolean isBypassed) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    if (userInitialCreditsExpiration == null) {
      userInitialCreditsExpiration = createInitialCreditsExpiration(user);
    }
    userInitialCreditsExpiration.setBypassed(isBypassed);
  }

  public boolean isExpirationBypassed(DbUser user) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();

    // After initial credit expiration is live,  userInitialCreditsExpiration should never be null
    // except for users who have not yet finished training. Could we safely remove this?
    return userInitialCreditsExpiration != null
        && (userInitialCreditsExpiration.isBypassed()
            || institutionService.shouldBypassForCreditsExpiration(user));
  }

  public DbUser extendInitialCreditsExpiration(DbUser user) {
    if (!workbenchConfigProvider.get().featureFlags.enableInitialCreditsExpiration) {
      throw new BadRequestException("Initial credits extension is disabled.");
    }
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();
    // This handles the case existing users that have not yet been migrated but also those who have
    // not yet completed RT training.
    if (userInitialCreditsExpiration == null) {
      throw new BadRequestException(
          "User does not have initial credits expiration set, so they cannot extend their expiration date.");
    }

    if (institutionService.shouldBypassForCreditsExpiration(user)) {
      throw new BadRequestException(
          "User has their initial credits expiration bypassed by their institution, and therefore cannot have their expiration extended.");
    }

    if (userInitialCreditsExpiration.isBypassed()) {
      throw new BadRequestException(
          "User has their initial credits expiration bypassed, and therefore cannot have their expiration extended.");
    }

    if (userInitialCreditsExpiration.getExtensionTime() != null) {
      throw new BadRequestException(
          "User has already extended their initial credits expiration and cannot extend further.");
    }
    if (!areCreditsExpiringSoon(user)) {
      throw new BadRequestException(
          "User's initial credits are not close enough to their expiration date to be extended.");
    }
    userInitialCreditsExpiration.setExpirationTime(
        new Timestamp(
            userInitialCreditsExpiration.getCreditStartTime().getTime()
                + TimeUnit.DAYS.toMillis(
                    workbenchConfigProvider.get().billing.initialCreditsExtensionPeriodDays)));
    userInitialCreditsExpiration.setExtensionTime(clockNow());
    return userDao.save(user);
  }

  public boolean checkInitialCreditsExtensionEligibility(DbUser dbUser) {
    DbUserInitialCreditsExpiration initialCreditsExpiration =
        dbUser.getUserInitialCreditsExpiration();
    Instant now = clock.instant();
    WorkbenchConfig.BillingConfig billingConfig = workbenchConfigProvider.get().billing;

    return userHasRemainingInitialCredits(dbUser)
        && initialCreditsExpiration != null
        && initialCreditsExpiration.getExtensionTime() == null
        && initialCreditsExpiration.getCreditStartTime() != null
        && now.isAfter(
            initialCreditsExpiration
                .getExpirationTime()
                .toInstant()
                .minus(billingConfig.initialCreditsExpirationWarningDays, DAYS))
        && now.isBefore(
            initialCreditsExpiration
                .getCreditStartTime()
                .toInstant()
                .plus(billingConfig.initialCreditsExtensionPeriodDays, DAYS));
  }

  private void checkExpiration(DbUser user) {
    DbUserInitialCreditsExpiration userInitialCreditsExpiration =
        user.getUserInitialCreditsExpiration();

    if (areUserCreditsExpired(user)) {
      handleExpiredCredits(user, userInitialCreditsExpiration);
    } else if (areCreditsExpiringSoon(user)
        && null == userInitialCreditsExpiration.getApproachingExpirationNotificationTime()) {
      handleExpiringSoonCredits(user, userInitialCreditsExpiration);
    }
  }

  private void handleExpiredCredits(
      DbUser user, DbUserInitialCreditsExpiration userInitialCreditsExpiration) {
    logger.info(
        "Initial credits expired for user {}. Expiration time: {}",
        user.getUsername(),
        userInitialCreditsExpiration.getExpirationTime());

    getWorkspacesForUser(user).stream()
        .filter(
            ws ->
                BillingUtils.isInitialCredits(
                    ws.getBillingAccountName(), workbenchConfigProvider.get()))
        .forEach(this::stopInitialCreditSpendInWorkspace);

    userInitialCreditsExpiration.setExpirationCleanupTime(clockNow());
    userDao.save(user);
  }

  private void handleExpiringSoonCredits(
      DbUser user, DbUserInitialCreditsExpiration userInitialCreditsExpiration) {
    logger.info(
        "Initial credits expiring soon for user {}. Expiration time: {}",
        user.getUsername(),
        userInitialCreditsExpiration.getExpirationTime());
    try {
      mailService.alertUserInitialCreditsExpiring(user);
      userInitialCreditsExpiration.setApproachingExpirationNotificationTime(clockNow());
      userDao.save(user);
    } catch (MessagingException e) {
      logger.error(
          "Failed to send initial credits expiration warning notification for user {}",
          user.getUserId());
    }
  }

  private void stopInitialCreditSpendInWorkspace(Workspace workspace) {
    String namespace = workspace.getNamespace();
    String googleProject = workspace.getGoogleProject();
    try {
      leonardoApiClient.deleteAllResources(googleProject, false);
      logger.info("Deleted apps and runtimes for workspace {}", namespace);
    } catch (WorkbenchException e) {
      logger.error("Failed to delete apps and runtimes for workspace {}", namespace, e);
    }
    if (workbenchConfigProvider.get().featureFlags.enableUnlinkBillingForInitialCredits) {
      try {
        fireCloudService.removeBillingAccountFromBillingProjectAsService(namespace);
        logger.info("Removed initial credits billing account from workspace {}", namespace);
      } catch (WorkbenchException e) {
        logger.error(
            "Failed to remove initial credits billing account from workspace {}", namespace, e);
      }
    }
  }

  private Timestamp clockNow() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

  @NotNull
  private List<WorkspaceCostView> getNotRecentlyUpdatedDbCostsForUsers(Set<DbUser> users) {
    List<WorkspaceCostView> allCostsInDbForUsers = workspaceDao.getWorkspaceCostViews(users);
    return findWorkspaceInitialCreditsUsagesThatWereNotRecentlyUpdated(allCostsInDbForUsers);
  }

  public void updateInitialCreditsExhaustion(DbUser user, boolean exhausted) {
    Iterable<DbWorkspace> toUpdate =
        () ->
            workspaceDao.findAllByCreator(user).stream()
                .filter(
                    ws ->
                        isInitialCredits(ws.getBillingAccountName(), workbenchConfigProvider.get()))
                .map(ws -> ws.setInitialCreditsExhausted(exhausted))
                .iterator();
    workspaceDao.saveAll(toUpdate);
  }

  /**
   * Filter the costs further by getting the workspaces that are active, or deleted but their
   * initial credits last updated time is before the workspace last updated time. This filtration
   * ensures that BQ will not be queried unnecessarily for the costs of deleted workspaces that we
   * already have their latest costs The method will return the workspace in either of these cases:
   *
   * <ol>
   *   <li>The workspace is active
   *   <li>The workspace is deleted within the past 6 months and any of the following is true.
   *       <ol>
   *         <li>Initial Credits Usage last updated time is null. This means that it wasn't
   *             calculated before
   *         <li>Workspace last updated time is null. This means we don't have enough info about the
   *             workspace, so we need to get its cost from BQ
   *         <li>Initial Credits Usage time is before the Workspace last updated time (Here the
   *             workspace last updated time will be the time that the workspace was deleted). This
   *             means that the workspace got changed some time after our last calculation, so we
   *             need to recalculate its usage
   *         <li>Initial Credits Usage time is after the Workspace last updated time (Here the
   *             workspace last updated time will be the time that the workspace was deleted), but
   *             the difference is smaller than a certain value. This case to account for charges
   *             that may occur after the workspace gets deleted and after the last cron had run.
   *       </ol>
   * </ol>
   *
   * @param allCostsInDbForUsers
   * @return a {@link java.util.List} of the workspaces that require updates only
   */
  @NotNull
  private Map<String, Long> findWorkspacesThatRequireUpdates(
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
                            && (c.getInitialCreditsLastUpdated() == null
                                || c.getWorkspaceLastUpdated() == null
                                || c.getInitialCreditsLastUpdated()
                                    .before(c.getWorkspaceLastUpdated())
                                || (c.getInitialCreditsLastUpdated()
                                        .after(c.getWorkspaceLastUpdated())
                                    && c.getInitialCreditsLastUpdated().getTime()
                                            - c.getWorkspaceLastUpdated().getTime()
                                        < Duration.ofDays(
                                                workbenchConfigProvider.get()
                                                    .billing
                                                    .numberOfDaysToConsiderForInitialCreditsUsageUpdate)
                                            .toMillis()))))
            .toList();

    logger.info(
        String.format("Workspaces that require updates: %d", workspacesThatRequireUpdate.size()));

    return workspacesThatRequireUpdate.stream()
        .collect(
            Collectors.toMap(
                WorkspaceCostView::getGoogleProject, WorkspaceCostView::getWorkspaceId));
  }

  /**
   * Get only the workspaces whose initial credits usages were updated before the configured time.
   * This insures that we don't calculate the costs unnecessarily again if we run the job manually
   * to clear up the backlog
   *
   * @param allCostsInDbForUsers a List of {@link WorkspaceCostView} which contains all info about
   *     workspaces including when they were last updated.
   * @return A filtered list containing the workspaces' initial credits usage entries that were not
   *     last updated in the last 60 minutes.
   */
  @NotNull
  private List<WorkspaceCostView> findWorkspaceInitialCreditsUsagesThatWereNotRecentlyUpdated(
      List<WorkspaceCostView> allCostsInDbForUsers) {
    Timestamp minusMinutes =
        Timestamp.valueOf(
            LocalDateTime.now().minusMinutes(getMinutesBeforeLastInitialCreditsJob()));

    return allCostsInDbForUsers.stream()
        .filter(
            c ->
                c.getInitialCreditsLastUpdated() == null
                    || c.getInitialCreditsLastUpdated().before(minusMinutes))
        .toList();
  }

  /**
   * Use the live cost from BQ to update the workspace initial credits usage in the DB.
   *
   * @param workspaceCostViews List of {@link WorkspaceCostView} containing all workspaces for the
   *     current batch of users
   * @param workspaceByProject A map where the keys are the Google Cloud project IDs of the
   *     workspaces and the values are the workspace IDs.
   * @return a Map of all live costs.
   */
  private void updateInitialCreditsUsageInDb(
      List<WorkspaceCostView> workspaceCostViews,
      Map<String, Double> allCostsInBqByProject,
      Map<String, Long> workspaceByProject) {

    // Cache that maps a workspace ID to its current cost in the database
    final Map<Long, Double> dbCostByWorkspace =
        workspaceCostViews.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceCostView::getWorkspaceId,
                    v -> Optional.ofNullable(v.getInitialCreditsCost()).orElse(0.0)));

    workspaceInitialCreditUsageService.updateWorkspaceInitialCreditsUsageInDB(
        dbCostByWorkspace, allCostsInBqByProject, workspaceByProject);
  }

  private boolean costAboveLimit(final DbUser user, final double currentCost) {
    return CostComparisonUtils.costAboveLimit(
        user, currentCost, workbenchConfigProvider.get().billing.defaultInitialCreditsDollarLimit);
  }

  private Integer getMinutesBeforeLastInitialCreditsJob() {
    return Optional.ofNullable(
            workbenchConfigProvider.get().billing.minutesBeforeLastInitialCreditsJob)
        .orElse(120);
  }

  private Map<String, Long> getWorkspaceByProjectCache(
      List<WorkspaceCostView> allCostsInDbForUsers) {
    // No need to proceed since there's nothing to update anyway
    if (allCostsInDbForUsers.isEmpty()) {
      return Collections.emptyMap();
    }

    // RW-14649: VWB workspaces may not have Google Projects
    Map<Boolean, List<WorkspaceCostView>> partitionedByGoogleProjectPresence =
        allCostsInDbForUsers.stream()
            .collect(Collectors.partitioningBy(w -> w.getGoogleProject() != null));

    List<WorkspaceCostView> missingProjects = partitionedByGoogleProjectPresence.get(false);
    if (!missingProjects.isEmpty()) {
      logger.info(
          String.format(
              "checkInitialCreditsUsageForUsers: Workspace IDs with null Google Project: %s",
              missingProjects.stream()
                  .map(WorkspaceCostView::getWorkspaceId)
                  .sorted()
                  .map(workspaceId -> Long.toString(workspaceId))
                  .collect(Collectors.joining(","))));
    }

    final Map<String, Long> workspaceByProject =
        findWorkspacesThatRequireUpdates(partitionedByGoogleProjectPresence.get(true));

    logger.info(
        String.format(
            "checkInitialCreditsUsageForUsers: Workspace IDs that require updates: %s",
            workspaceByProject.values().stream()
                .sorted()
                .map(workspaceId -> Long.toString(workspaceId))
                .collect(Collectors.joining(","))));

    return workspaceByProject;
  }

  @NotNull
  private static Map<Long, Double> getLiveCostByCreatorCache(
      Map<String, Double> liveCostsInBQ,
      Map<String, Long> workspaceByProject,
      Map<Long, Long> creatorByWorkspace) {
    return liveCostsInBQ.entrySet().stream()
        .filter(e -> workspaceByProject.containsKey(e.getKey()))
        .collect(
            Collectors.groupingBy(
                e -> creatorByWorkspace.get(workspaceByProject.get(e.getKey())),
                Collectors.summingDouble(Entry::getValue)));
  }

  @NotNull
  private static Map<Long, Long> getCreatorByWorkspaceCache(
      List<WorkspaceCostView> allCostsInDbForUsers) {
    return allCostsInDbForUsers.stream()
        .collect(
            Collectors.toMap(WorkspaceCostView::getWorkspaceId, WorkspaceCostView::getCreatorId));
  }

  @NotNull
  private static Map<Long, Double> getDbCostByCreatorCache(
      List<WorkspaceCostView> allCostsInDbForUsers) {
    final Map<Long, Double> dbCostByCreator =
        allCostsInDbForUsers.stream()
            .collect(
                Collectors.groupingBy(
                    WorkspaceCostView::getCreatorId,
                    Collectors.summingDouble(
                        v -> Optional.ofNullable(v.getInitialCreditsCost()).orElse(0.0))));
    return dbCostByCreator;
  }

  private List<Workspace> getWorkspacesForUser(DbUser user) {
    return workspaceMapper
        .toApiWorkspaceResponseList(workspaceDao, fireCloudService.listWorkspacesAsService(), this)
        .stream()
        .map(WorkspaceResponse::getWorkspace)
        .filter(ws -> ws.getCreatorUser().getUserName().equals(user.getUsername()))
        .collect(Collectors.toList());
  }
}
