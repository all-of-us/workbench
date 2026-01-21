package org.pmiops.workbench.initialcredits;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to call the {@link InitialCreditsService} with batches of users. This ensures that
 * InitialCreditsBatchUpdateService will commit the transaction for a smaller batch of users instead
 * of processing all users in one transaction and eventually timing out. See RW-6280
 */
@Service
public class InitialCreditsBatchUpdateService {
  private static final Logger log =
      Logger.getLogger(InitialCreditsBatchUpdateService.class.getName());

  private final InitialCreditsBigQueryService initialCreditsBigQueryService;
  private final InitialCreditsService initialCreditsService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public InitialCreditsBatchUpdateService(
      InitialCreditsBigQueryService initialCreditsBigQueryService,
      InitialCreditsService initialCreditsService,
      Provider<Stopwatch> stopwatchProvider,
      UserDao userDao,
      WorkspaceDao workspaceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.initialCreditsBigQueryService = initialCreditsBigQueryService;
    this.initialCreditsService = initialCreditsService;
    this.userDao = userDao;
    this.stopwatchProvider = stopwatchProvider;
    this.workspaceDao = workspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Takes in List of user Id and then find its BQ Cost
   *
   * @param userIdList
   */
  public void checkInitialCreditsUsage(List<Long> userIdList) {

    // Get the list of users from the database based on the userIdList.
    List<DbUser> users = userDao.findUsersByUserIdIn(userIdList);

    // This returns a map of google project id to its live cost in BigQuery.
    Map<String, Double> terraWorkspacesLiveCosts = findTerraWorkspacesLiveCosts(userIdList);

    // This is a map of user_id to its live total cost in BigQuery for VWB projects. We don't need
    // the google project id here because we don't store it anywhere in the database and therefore
    // it doesn't give us any information.
    Map<Long, Double> vwbUserLiveCosts = findVwbUsersLiveCosts(users);

    Set<DbUser> dbUserSet = Sets.newHashSet(users);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        dbUserSet, terraWorkspacesLiveCosts, vwbUserLiveCosts);
  }

  private @NotNull Map<String, Double> findTerraWorkspacesLiveCosts(List<Long> userIdList) {
    Set<String> googleProjects = workspaceDao.getWorkspaceGoogleProjectsForCreators(userIdList);
    Stopwatch stopwatch = stopwatchProvider.get().start();
    Map<String, Double> userWorkspaceCosts =
        initialCreditsBigQueryService.getAllTerraWorkspaceCostsFromBQ().entrySet().stream()
            .filter(entry -> googleProjects.contains(entry.getKey()))
            .collect(
                Collectors.groupingBy(Entry::getKey, Collectors.summingDouble(Entry::getValue)));
    Duration elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "checkInitialCreditsUsage: Filtered %d workspace cost entries from BigQuery in %s",
            userWorkspaceCosts.size(), formatDurationPretty(elapsed)));
    return userWorkspaceCosts;
  }

  /** Return a map of user_id -> cost for VWB pods */
  private Map<Long, Double> findVwbUsersLiveCosts(List<DbUser> users) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBInitialCreditsExhaustion) {
      return Collections.emptyMap();
    }
    // Filter and collect active VWB user pods
    Map<String, DbVwbUserPod> activePodIdToUserPodMap =
        users.stream()
            .map(DbUser::getVwbUserPod)
            .filter(Objects::nonNull)
            .filter(DbVwbUserPod::isInitialCreditsActive)
            .filter(pod -> pod.getVwbPodId() != null) // Filter out lock rows with null pod_id
            .collect(Collectors.toMap(DbVwbUserPod::getVwbPodId, pod -> pod));

    // Extract the set of active VWB pod IDs
    Set<String> activeVwbPodIds = activePodIdToUserPodMap.keySet();

    // Map VWB project costs to user IDs
    return initialCreditsBigQueryService.getAllVWBProjectCostsFromBQ().entrySet().stream()
        .filter(entry -> activeVwbPodIds.contains(entry.getKey()))
        .collect(
            Collectors.toMap(
                entry -> activePodIdToUserPodMap.get(entry.getKey()).getUser().getUserId(),
                Entry::getValue));
  }
}
