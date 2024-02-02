package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.CostComparisonUtils.getUserFreeTierDollarLimit;

import com.google.common.collect.Sets;
import jakarta.mail.MessagingException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ExpiredFreeCreditsEventRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskFreeCreditExpiry implements CloudTaskFreeCreditExpiryApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(CloudTaskFreeCreditExpiry.class);

  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceService workspaceService;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfig;
  private final LeonardoApiClient leonardoApiClient;
  private final MailService mailService;

  CloudTaskFreeCreditExpiry(
      ImpersonatedWorkspaceService impersonatedWorkspaceService,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfig1,
      LeonardoApiClient leonardoApiClient,
      MailService mailService) {
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
    this.workspaceDao = workspaceDao;
    this.workspaceService = workspaceService;
    this.userDao = userDao;
    this.workbenchConfig = workbenchConfig1;
    this.leonardoApiClient = leonardoApiClient;
    this.mailService = mailService;
  }

  @Override
  @SuppressWarnings("unchecked")
  public ResponseEntity<Void> handleFreeCreditsExpiry(ExpiredFreeCreditsEventRequest request) {

    if (request.getUsers().isEmpty()) {
      logger.warn("users are empty");
      return ResponseEntity.badRequest().build();
    }

    Iterable<DbUser> users = (Iterable<DbUser>) userDao.findAllById(request.getUsers());
    Map<Long, Double> dbCostByCreator = (Map<Long, Double>) request.getDbCostByCreator();
    Map<Long, Double> liveCostByCreator = (Map<Long, Double>) request.getLiveCostByCreator();
    Set<DbUser> usersSet =
        StreamSupport.stream(users.spliterator(), false).collect(Collectors.toSet());
    var newlyExpiredUsers = getNewlyExpiredUsers(usersSet, dbCostByCreator, liveCostByCreator);

    handleExpiredUsers(newlyExpiredUsers);

    alertUsersBasedOnTheThreshold(dbCostByCreator, liveCostByCreator, newlyExpiredUsers);

    return ResponseEntity.noContent().build();
  }

  private void handleExpiredUsers(Set<DbUser> newlyExpiredUsers) {
    newlyExpiredUsers.forEach(
        user -> {
          logger.info("Free tier Billing Service: Sending email to user {}", user.getUsername());
          workspaceService.updateFreeTierWorkspacesStatus(user, BillingStatus.INACTIVE);
          // delete apps and runtimes
          deleteAppsAndRuntimesInFreeTierWorkspaces(user);
          try {
            mailService.alertUserInitialCreditsExpiration(user);
          } catch (MessagingException e) {
            logger.warn("failed to mail free tier expiration email to {}", user.getUsername(), e);
          }
        });
  }

  private void alertUsersBasedOnTheThreshold(
      Map<Long, Double> dbCostByCreator,
      Map<Long, Double> liveCostByCreator,
      Set<DbUser> newlyExpiredUsers) {
    final List<Double> costThresholdsInDescOrder =
        workbenchConfig.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    Map<Long, DbUser> usersCache =
        StreamSupport.stream(userDao.findAllById(liveCostByCreator.keySet()).spliterator(), false)
            .collect(Collectors.toMap(DbUser::getUserId, Function.identity()));

    // Filter out the users who have recently expired because we already alerted them
    Map<Long, Double> filteredLiveCostByCreator =
        liveCostByCreator.entrySet().stream()
            .filter(entry -> !newlyExpiredUsers.contains(usersCache.get(entry.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    filteredLiveCostByCreator.forEach(
        (userId, currentCost) -> {
          final double previousCost = dbCostByCreator.getOrDefault(userId, 0.0);
          maybeAlertOnCostThresholds(
              usersCache.get(userId),
              Math.max(currentCost, previousCost),
              previousCost,
              costThresholdsInDescOrder);
        });
  }

  /**
   * Get the list of newly expired users (who exceeded their free tier limit) and mark all their
   * workspaces as inactive
   *
   * @param allUsers set of all users to filter them whether they have active free tier workspace
   * @param dbCostByCreator Map of userId->dbCost
   * @param liveCostByCreator Map of userId->liveCost
   * @return a {@link Set} of newly expired users
   */
  private Set<DbUser> getNewlyExpiredUsers(
      final Set<DbUser> allUsers,
      Map<Long, Double> dbCostByCreator,
      Map<Long, Double> liveCostByCreator) {

    final Map<Long, DbUser> dbUsersWithChangedCosts =
        findDbUsersWithChangedCosts(dbCostByCreator, liveCostByCreator);
    Set<DbUser> freeTierUsers = getFreeTierActiveWorkspaceCreatorsIn(allUsers);

    // Find users who exceeded their free tier limit
    // Here costs in liveCostByCreator could be outdated because we're filtering on active  or
    // recently deleted workspaces in previous steps.
    // However, dbCostByCreator will contain the up-to-date costs for all the
    // other workspaces. This is why Math.max is used
    final Set<DbUser> expiredUsers =
        dbUsersWithChangedCosts.entrySet().stream()
            .filter(
                e ->
                    CostComparisonUtils.costAboveLimit(
                        e.getValue(),
                        Math.max(
                            dbCostByCreator.get(e.getKey()), liveCostByCreator.get(e.getKey())),
                        workbenchConfig.get().billing.defaultFreeCreditsDollarLimit))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

    final Set<DbUser> newlyExpiredFreeTierUsers = Sets.intersection(expiredUsers, freeTierUsers);

    logger.info(
        String.format(
            "Found %d users exceeding their free tier limit, out of which, %d are new",
            expiredUsers.size(), newlyExpiredFreeTierUsers.size()));

    return newlyExpiredFreeTierUsers;
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

    Map<Long, DbUser> dbUsersWithChangedCosts =
        StreamSupport.stream(userDao.findAllById(usersWithChangedCosts).spliterator(), false)
            .collect(Collectors.toMap(DbUser::getUserId, Function.identity()));
    return dbUsersWithChangedCosts;
  }

  private Set<DbUser> getFreeTierActiveWorkspaceCreatorsIn(Set<DbUser> users) {
    return workspaceDao.findCreatorsByBillingStatusAndBillingAccountNameIn(
        BillingStatus.ACTIVE,
        new ArrayList<>(workbenchConfig.get().billing.freeTierBillingAccountNames()),
        users);
  }

  private void deleteAppsAndRuntimesInFreeTierWorkspaces(DbUser user) {
    logger.info("Deleting apps and runtimes for user " + user.getUsername());

    impersonatedWorkspaceService.getOwnedWorkspaces(user.getUsername()).stream()
        .map(WorkspaceResponse::getWorkspace)
        .map(Workspace::getNamespace)
        .map(workspaceDao::getByNamespace)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(
            dbWorkspace ->
                workbenchConfig
                    .get()
                    .billing
                    .freeTierBillingAccountNames()
                    .contains(dbWorkspace.getBillingAccountName()))
        .forEach(
            dbWorkspace -> {
              String namespace = dbWorkspace.getWorkspaceNamespace();
              leonardoApiClient.deleteAllResources(dbWorkspace.getGoogleProject(), false);
              logger.info("Deleting apps and runtimes for workspace " + namespace);
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

    final double limit =
        getUserFreeTierDollarLimit(
            user, workbenchConfig.get().billing.defaultFreeCreditsDollarLimit);
    final double remainingBalance = limit - currentCost;

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
      logger.warn(msg);
    }

    final double currentFraction = currentCost / limit;
    final double previousFraction = previousCost / limit;

    for (final double threshold : thresholdsInDescOrder) {
      if (CostComparisonUtils.compareCostFractions(currentFraction, threshold) > 0) {
        // only alert if we have not done so previously
        if (CostComparisonUtils.compareCostFractions(previousFraction, threshold) <= 0) {
          try {
            mailService.alertUserInitialCreditsDollarThreshold(
                user, threshold, currentCost, remainingBalance);
          } catch (final MessagingException e) {
            logger.warn("failed to mail threshold email", e);
          }
        }

        // break out here to ensure we don't alert for lower thresholds
        break;
      }
    }
  }
}
