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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitialCreditsBatchUpdateServiceTest {

  @Autowired private InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;

  @MockBean private WorkspaceDao mockWorkspaceDao;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private UserDao mockUserDao;
  @MockBean private InitialCreditsBigQueryService mockBigQueryService;

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
    mockVwbUserCost();
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(
            mockDbUserSet, /* Terra costs */ Collections.emptyMap(), getUsersCostMap());
  }

  @Test
    public void checkInitialCreditsUsage_bothTerraAndVWBProjects() {
        mockGoogleProjectCost();
        mockVwbUserCost();
        initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

        verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

        verify(mockInitialCreditsService)
            .checkInitialCreditsUsageForUsers(
                mockDbUserSet, getWorkspacesCostMap(), getUsersCostMap());
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

}
