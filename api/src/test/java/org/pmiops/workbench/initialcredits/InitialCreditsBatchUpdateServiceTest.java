package org.pmiops.workbench.initialcredits;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitialCreditsBatchUpdateServiceTest {

  @Autowired private InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;

  @MockitoBean private WorkspaceDao mockWorkspaceDao;
  @MockitoBean private InitialCreditsService mockInitialCreditsService;
  @MockitoBean private UserDao mockUserDao;
  @MockitoBean private InitialCreditsBigQueryService mockBigQueryService;

  private static WorkbenchConfig config;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    InitialCreditsBatchUpdateService.class,
    Stopwatch.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  Set<DbUser> mockDbUserSet = new HashSet<>();

  Set<String> googleProjectIdsSet = new HashSet<>(List.of("12", "22", "23", "32", "33"));

  @BeforeEach
  public void init() {
    config = WorkbenchConfig.createEmptyConfig();
    mockDbUsers();
    mockGoogleProjectsForUser();
  }

  @Test
  public void checkInitialCreditsUsage_onlyTerraWorkspaces() {
    mockGoogleProjectCost();
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            mockDbUserSet, getWorkspacesCostMap(), /* VWB costs */ Collections.emptyMap());
  }

  @Test
  public void checkInitialCreditsUsage_onlyVWBProjects() {
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;
    mockVwbUserCost();
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            mockDbUserSet, /* Terra costs */ Collections.emptyMap(), getUsersCostMap());
  }

  @Test
  public void checkInitialCreditsUsage_bothTerraAndVWBProjects() {
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;
    mockGoogleProjectCost();
    mockVwbUserCost();
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(mockDbUserSet, getWorkspacesCostMap(), getUsersCostMap());
  }

  @Test
  public void checkInitialCreditsUsage_includesInactiveVWBPods() {
    // Test that VWB pods with initialCreditsActive = false are still included in cost calculations
    // This is important for handling delayed billing reports from Google
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;

    // Create users with inactive VWB pods
    Set<DbUser> usersWithInactivePods = new HashSet<>();
    DbUser userWithInactivePod = new DbUser().setUserId(4L);
    DbVwbUserPod inactivePod =
        new DbVwbUserPod()
            .setVwbUserPodId(4L)
            .setUser(userWithInactivePod)
            .setVwbPodId("pod4")
            .setInitialCreditsActive(false); // Inactive pod
    userWithInactivePod.setVwbUserPod(inactivePod);
    usersWithInactivePods.add(userWithInactivePod);

    when(mockUserDao.findUsersByUserIdIn(List.of(4L))).thenReturn(List.of(userWithInactivePod));
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(4L)))
        .thenReturn(Collections.emptySet());
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ()).thenReturn(Map.of("pod4", 5.5d));

    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(4L));

    // Verify that the cost for the inactive pod is still included
    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            usersWithInactivePods, Collections.emptyMap(), Map.of(4L, 5.5d));
  }

  @Test
  public void checkInitialCreditsUsage_mixedActiveAndInactivePods() {
    // Test with a mix of active and inactive VWB pods
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;

    Set<DbUser> mixedUsers = new HashSet<>();

    // User with active pod
    DbUser userActive = new DbUser().setUserId(5L);
    DbVwbUserPod activePod =
        new DbVwbUserPod()
            .setVwbUserPodId(5L)
            .setUser(userActive)
            .setVwbPodId("pod5")
            .setInitialCreditsActive(true);
    userActive.setVwbUserPod(activePod);
    mixedUsers.add(userActive);

    // User with inactive pod
    DbUser userInactive = new DbUser().setUserId(6L);
    DbVwbUserPod inactivePod =
        new DbVwbUserPod()
            .setVwbUserPodId(6L)
            .setUser(userInactive)
            .setVwbPodId("pod6")
            .setInitialCreditsActive(false);
    userInactive.setVwbUserPod(inactivePod);
    mixedUsers.add(userInactive);

    when(mockUserDao.findUsersByUserIdIn(List.of(5L, 6L)))
        .thenReturn(List.of(userActive, userInactive));
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(5L, 6L)))
        .thenReturn(Collections.emptySet());
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ())
        .thenReturn(Map.of("pod5", 10.0d, "pod6", 20.0d));

    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(5L, 6L));

    // Verify that costs for both active and inactive pods are included
    Map<Long, Double> expectedCosts = Map.of(5L, 10.0d, 6L, 20.0d);
    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(mixedUsers, Collections.emptyMap(), expectedCosts);
  }

  @Test
  public void checkInitialCreditsUsage_excludesNullPodId() {
    // Test that pods with null pod_id (lock rows) are properly excluded
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;

    Set<DbUser> usersWithNullPodId = new HashSet<>();

    // User with null pod_id (lock row)
    DbUser userWithNullPodId = new DbUser().setUserId(7L);
    DbVwbUserPod nullPodIdPod =
        new DbVwbUserPod()
            .setVwbUserPodId(7L)
            .setUser(userWithNullPodId)
            .setVwbPodId(null) // Null pod_id indicates a lock row
            .setInitialCreditsActive(true);
    userWithNullPodId.setVwbUserPod(nullPodIdPod);
    usersWithNullPodId.add(userWithNullPodId);

    // User with valid pod_id
    DbUser userWithValidPodId = new DbUser().setUserId(8L);
    DbVwbUserPod validPod =
        new DbVwbUserPod()
            .setVwbUserPodId(8L)
            .setUser(userWithValidPodId)
            .setVwbPodId("pod8")
            .setInitialCreditsActive(true);
    userWithValidPodId.setVwbUserPod(validPod);
    usersWithNullPodId.add(userWithValidPodId);

    when(mockUserDao.findUsersByUserIdIn(List.of(7L, 8L)))
        .thenReturn(List.of(userWithNullPodId, userWithValidPodId));
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(7L, 8L)))
        .thenReturn(Collections.emptySet());
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ()).thenReturn(Map.of("pod8", 15.0d));

    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(7L, 8L));

    // Verify that only the user with valid pod_id has costs included
    Map<Long, Double> expectedCosts = Map.of(8L, 15.0d);
    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            usersWithNullPodId, Collections.emptyMap(), expectedCosts);
  }

  @Test
  public void checkInitialCreditsUsage_handlesDelayedBillingReport() {
    // Test scenario where billing report is delayed from Google
    // User has exhausted credits (active=false) but new costs appear in BQ
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;

    Set<DbUser> exhaustedUsers = new HashSet<>();

    // User who previously exhausted credits
    DbUser exhaustedUser = new DbUser().setUserId(9L);
    DbVwbUserPod exhaustedPod =
        new DbVwbUserPod()
            .setVwbUserPodId(9L)
            .setUser(exhaustedUser)
            .setVwbPodId("pod9")
            .setInitialCreditsActive(false) // Previously marked as exhausted
            .setCost(100.0); // Previous cost in DB
    exhaustedUser.setVwbUserPod(exhaustedPod);
    exhaustedUsers.add(exhaustedUser);

    when(mockUserDao.findUsersByUserIdIn(List.of(9L))).thenReturn(List.of(exhaustedUser));
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(9L)))
        .thenReturn(Collections.emptySet());
    // New costs appear in BQ due to delayed billing report
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ()).thenReturn(Map.of("pod9", 150.0d));

    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(9L));

    // Verify that the updated cost from BQ is included despite inactive status
    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            exhaustedUsers, Collections.emptyMap(), Map.of(9L, 150.0d));
  }

  @Test
  public void checkInitialCreditsUsage_verifiesInactivePodsIncluded() {
    // This test specifically validates that the removal of the initialCreditsActive filter
    // allows inactive pods to be included in cost calculations - addressing the issue
    // where delayed billing reports from Google could cause cost mismatches
    config.featureFlags.enableVWBInitialCreditsExhaustion = true;

    Set<DbUser> testUsers = new HashSet<>();

    // Create multiple users with different pod states
    DbUser userWithActiveAndCost = new DbUser().setUserId(10L);
    DbVwbUserPod activePodWithCost =
        new DbVwbUserPod()
            .setVwbUserPodId(10L)
            .setUser(userWithActiveAndCost)
            .setVwbPodId("pod10")
            .setInitialCreditsActive(true)
            .setCost(50.0);
    userWithActiveAndCost.setVwbUserPod(activePodWithCost);
    testUsers.add(userWithActiveAndCost);

    DbUser userWithInactiveAndCost = new DbUser().setUserId(11L);
    DbVwbUserPod inactivePodWithCost =
        new DbVwbUserPod()
            .setVwbUserPodId(11L)
            .setUser(userWithInactiveAndCost)
            .setVwbPodId("pod11")
            .setInitialCreditsActive(false) // Inactive but should still be included
            .setCost(75.0);
    userWithInactiveAndCost.setVwbUserPod(inactivePodWithCost);
    testUsers.add(userWithInactiveAndCost);

    DbUser userWithInactiveNoCost = new DbUser().setUserId(12L);
    DbVwbUserPod inactivePodNoCost =
        new DbVwbUserPod()
            .setVwbUserPodId(12L)
            .setUser(userWithInactiveNoCost)
            .setVwbPodId("pod12")
            .setInitialCreditsActive(false)
            .setCost(0.0);
    userWithInactiveNoCost.setVwbUserPod(inactivePodNoCost);
    testUsers.add(userWithInactiveNoCost);

    when(mockUserDao.findUsersByUserIdIn(List.of(10L, 11L, 12L)))
        .thenReturn(
            List.of(userWithActiveAndCost, userWithInactiveAndCost, userWithInactiveNoCost));
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(10L, 11L, 12L)))
        .thenReturn(Collections.emptySet());

    // BigQuery returns costs for all pods regardless of active status
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ())
        .thenReturn(
            Map.of(
                "pod10", 55.0d, // Active pod with updated cost
                "pod11", 85.0d, // Inactive pod with updated cost (delayed billing)
                "pod12", 10.0d // Inactive pod with new cost (delayed billing)
                ));

    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(10L, 11L, 12L));

    // All pods should have their costs included, not just active ones
    Map<Long, Double> expectedCosts =
        Map.of(
            10L, 55.0d,
            11L, 85.0d,
            12L, 10.0d);

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(testUsers, Collections.emptyMap(), expectedCosts);
  }

  private void mockDbUsers() {
    DbUser dbUserForId1 = new DbUser().setUserId(1L);
    DbUser dbUserForId2 = new DbUser().setUserId(2L);
    DbUser dbUserForId3 = new DbUser().setUserId(3L);

    DbVwbUserPod dbVwbUserPod1 =
        new DbVwbUserPod()
            .setVwbUserPodId(1L)
            .setUser(dbUserForId1)
            .setVwbPodId("pod1")
            .setInitialCreditsActive(true);
    dbUserForId1.setVwbUserPod(dbVwbUserPod1);
    DbVwbUserPod dbVwbUserPod2 =
        new DbVwbUserPod()
            .setVwbUserPodId(2L)
            .setUser(dbUserForId2)
            .setVwbPodId("pod2")
            .setInitialCreditsActive(true);
    dbUserForId2.setVwbUserPod(dbVwbUserPod2);
    DbVwbUserPod dbVwbUserPod3 =
        new DbVwbUserPod()
            .setVwbUserPodId(3L)
            .setUser(dbUserForId3)
            .setVwbPodId("pod3")
            .setInitialCreditsActive(true);
    dbUserForId3.setVwbUserPod(dbVwbUserPod3);

    mockDbUserSet.add(dbUserForId1);
    mockDbUserSet.add(dbUserForId2);
    mockDbUserSet.add(dbUserForId3);

    when(mockUserDao.findUsersByUserIdIn(List.of(1L, 2L, 3L)))
        .thenReturn(List.of(dbUserForId1, dbUserForId2, dbUserForId3));
  }

  private void mockGoogleProjectsForUser() {
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L)))
        .thenReturn(googleProjectIdsSet);
  }

  private void mockGoogleProjectCost() {
    when(mockBigQueryService.getAllTerraWorkspaceCostsFromBQ())
        .thenReturn(Map.of("12", 0.013d, "22", 1.123d, "23", 6.5d, "32", 0.34d, "33", 0.9d));
  }

  private void mockVwbUserCost() {
    when(mockBigQueryService.getAllVWBProjectCostsFromBQ())
        .thenReturn(Map.of("pod1", 1.1d, "pod2", 2.2d, "pod3", 3.3d));
  }

  private Map<String, Double> getWorkspacesCostMap() {
    Map<String, Double> userCostMap = new HashMap<String, Double>();
    userCostMap.put("12", 0.013);
    userCostMap.put("22", 1.123);
    userCostMap.put("23", 6.5);
    userCostMap.put("32", 0.34);
    userCostMap.put("33", 0.9);
    return userCostMap;
  }

  private Map<Long, Double> getUsersCostMap() {
    Map<Long, Double> userCostMap = new HashMap<Long, Double>();
    userCostMap.put(1L, 1.1);
    userCostMap.put(2L, 2.2);
    userCostMap.put(3L, 3.3);
    return userCostMap;
  }

  @AfterEach
  public void tearDown() {
    config.featureFlags.enableVWBInitialCreditsExhaustion = false;
    mockUserDao.deleteAll();
  }
}
