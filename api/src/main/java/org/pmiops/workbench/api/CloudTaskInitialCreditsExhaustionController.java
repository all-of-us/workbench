package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;
import static org.pmiops.workbench.utils.CostComparisonUtils.getUserFreeTierDollarLimit;

import com.google.common.collect.Sets;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.ExhaustedInitialCreditsEventRequest;
import org.pmiops.workbench.utils.CostComparisonUtils;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskInitialCreditsExhaustionController
    implements CloudTaskInitialCreditExhaustionApiDelegate {

  private static final Logger logger =
      LoggerFactory.getLogger(CloudTaskInitialCreditsExhaustionController.class);

  private final WorkspaceDao workspaceDao;
  private final WorkspaceService workspaceService;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfig;
  private final LeonardoApiClient leonardoApiClient;
  private final MailService mailService;

  CloudTaskInitialCreditsExhaustionController(
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfig,
      LeonardoApiClient leonardoApiClient,
      MailService mailService) {
    this.workspaceDao = workspaceDao;
    this.workspaceService = workspaceService;
    this.userDao = userDao;
    this.workbenchConfig = workbenchConfig;
    this.leonardoApiClient = leonardoApiClient;
    this.mailService = mailService;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ResponseEntity<Void> handleInitialCreditsExhaustionBatch(
      ExhaustedInitialCreditsEventRequest request) {

    if (request.getUsers().isEmpty()) {
      logger.warn("users are empty");
      return ResponseEntity.badRequest().build();
    }

    logger.info(
        "handleInitialCreditsExhaustionBatch: Processing request for users: {}",
        request.getUsers().toString());

    Iterable<DbUser> users = userDao.findAllById(request.getUsers());
    Set<DbUser> usersSet =
        StreamSupport.stream(users.spliterator(), false).collect(Collectors.toSet());

    Map<String, Double> stringKeyDbCostMap = (Map<String, Double>) request.getDbCostByCreator();
    Map<Long, Double> dbCostByCreator = convertMapKeysToLong(stringKeyDbCostMap);

    Map<String, Double> stringKeyLiveCostMap = (Map<String, Double>) request.getLiveCostByCreator();
    Map<Long, Double> liveCostByCreator = convertMapKeysToLong(stringKeyLiveCostMap);

    var newlyExpiredUsers = getNewlyExpiredUsers(usersSet, dbCostByCreator, liveCostByCreator);

    handleExpiredUsers(newlyExpiredUsers);

    alertUsersBasedOnTheThreshold(usersSet, dbCostByCreator, liveCostByCreator, newlyExpiredUsers);

    logger.info(
        "handleInitialCreditsExhaustionBatch: Finished processing request for users: {}",
        request.getUsers().toString());

    return ResponseEntity.noContent().build();
  }

  private void handleExpiredUsers(Set<DbUser> newlyExpiredUsers) {
    newlyExpiredUsers.forEach(
        user -> {
          logger.info(
              "Free tier Billing Service: handling user with expired credits {}",
              user.getUsername());
          workspaceService.updateInitialCreditsExhaustion(user, true);
          // delete apps and runtimes
          deleteAppsAndRuntimesInFreeTierWorkspaces(user);
          try {
            mailService.alertUserInitialCreditsExhausted(user);
          } catch (MessagingException e) {
            logger.warn("failed to send free tier expiration email to {}", user.getUsername(), e);
          }
        });
  }

  private void alertUsersBasedOnTheThreshold(
      Set<DbUser> users,
      Map<Long, Double> dbCostByCreator,
      Map<Long, Double> liveCostByCreator,
      Set<DbUser> newlyExpiredUsers) {
    final List<Double> costThresholdsInDescOrder =
        workbenchConfig.get().billing.freeTierCostAlertThresholds;
    costThresholdsInDescOrder.sort(Comparator.reverseOrder());

    Map<Long, DbUser> usersCache =
        users.stream()
            .filter(u -> liveCostByCreator.containsKey(u.getUserId()))
            .collect(Collectors.toMap(DbUser::getUserId, Function.identity()));

    // Filter out the users who have recently expired because we already alerted them
    Map<Long, Double> filteredLiveCostByCreator =
        liveCostByCreator.entrySet().stream()
            .filter(entry -> !newlyExpiredUsers.contains(usersCache.get(entry.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    logger.info("Handling cost alerts for users: {}", usersCache.keySet());
    logger.info(
        "DB costs by creator: {}, live costs by creator: {}", dbCostByCreator, liveCostByCreator);

    filteredLiveCostByCreator.forEach(
        (userId, currentCost) -> {
          final double previousCost = dbCostByCreator.getOrDefault(userId, 0.0);
          maybeAlertOnCostThresholds(
              usersCache.containsKey(userId)
                  ? usersCache.get(userId)
                  : userDao.findUserByUserId(userId),
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
        findDbUsersWithChangedCosts(allUsers, dbCostByCreator, liveCostByCreator);
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
   * @param allUsers
   * @param dbCostByCreator Map of userId->dbCost
   * @param liveCostByCreator Map of userId->liveCost
   * @return a {@link Map} of user Ids to their DbUsers
   */
  @NotNull
  private Map<Long, DbUser> findDbUsersWithChangedCosts(
      Set<DbUser> allUsers,
      Map<Long, Double> dbCostByCreator,
      Map<Long, Double> liveCostByCreator) {
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
        allUsers.stream()
            .filter(u -> usersWithChangedCosts.contains(u.getUserId()))
            .collect(Collectors.toMap(DbUser::getUserId, Function.identity()));

    return dbUsersWithChangedCosts;
  }

  private Set<DbUser> getFreeTierActiveWorkspaceCreatorsIn(Set<DbUser> users) {
    return workspaceDao.findCreatorsByActiveInitialCredits(
        List.of(workbenchConfig.get().billing.initialCreditsBillingAccountName()), users);
  }

  private void deleteAppsAndRuntimesInFreeTierWorkspaces(DbUser user) {
    logger.info("Deleting apps and runtimes for user {}", user.getUsername());

    workspaceDao.findAllByCreator(user).stream()
        .filter(
            dbWorkspace ->
                isInitialCredits(dbWorkspace.getBillingAccountName(), workbenchConfig.get()))
        .filter(DbWorkspace::isActive)
        .forEach(
            dbWorkspace -> {
              String namespace = dbWorkspace.getWorkspaceNamespace();
              try {
                leonardoApiClient.deleteAllResources(dbWorkspace.getGoogleProject(), false);
                logger.info("Deleted apps and runtimes for workspace {}", namespace);
              } catch (WorkbenchException e) {
                logger.error("Failed to delete apps and runtimes for workspace {}", namespace, e);
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

  private Map<Long, Double> convertMapKeysToLong(Map<String, Double> stringKeyMap) {
    Map<Long, Double> longKeyMap = new HashMap<>();
    for (Map.Entry<String, Double> entry : stringKeyMap.entrySet()) {
      longKeyMap.put(Long.parseLong(entry.getKey()), entry.getValue());
    }
    return longKeyMap;
  }
}
