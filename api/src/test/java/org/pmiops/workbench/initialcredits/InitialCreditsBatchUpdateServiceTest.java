package org.pmiops.workbench.initialcredits;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Stopwatch;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.BigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
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

  @Autowired private WorkspaceDao mockWorkspaceDao;
  @Autowired private InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;
  @Autowired private InitialCreditsService mockInitialCreditsService;
  @Autowired private UserDao mockUserDao;

  private static WorkbenchConfig config;

  @TestConfiguration
  @Import({
    BigQueryConfig.class,
    BigQueryService.class,
    FakeClockConfiguration.class,
    InitialCreditsBatchUpdateService.class,
    Stopwatch.class,
  })
  @MockBean({UserDao.class, WorkspaceDao.class, InitialCreditsService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  Set<DbUser> mockDbuserSet = new HashSet<>();

  Set<String> googleProjectIdsSet = new HashSet<>(List.of("12", "22", "23", "32", "33"));

  @BeforeEach
  public void init() {
    config = WorkbenchConfig.createEmptyConfig();
    // config.billing = new WorkbenchConfig.BillingConfig();
    mockDbUser();
    mockGoogleProjectsForUser();
  }

  @Test
  public void testFreeTierBillingBatchUpdateService() {
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(Arrays.asList(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getGoogleProjectForUserList(Arrays.asList(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(mockDbuserSet, getUserCostMap());
  }

  private void mockDbUser() {
    DbUser dbUserForId1 = new DbUser().setUserId(1L);
    DbUser dbUserForId2 = new DbUser().setUserId(2L);
    DbUser dbUserForId3 = new DbUser().setUserId(3L);
    when(mockUserDao.findUserByUserId(1L)).thenReturn(dbUserForId1);
    when(mockUserDao.findUserByUserId(2L)).thenReturn(dbUserForId2);
    when(mockUserDao.findUserByUserId(3L)).thenReturn(dbUserForId3);
    mockDbuserSet.add(dbUserForId1);
    mockDbuserSet.add(dbUserForId2);
    mockDbuserSet.add(dbUserForId3);
  }

  private void mockGoogleProjectsForUser() {
    when(mockWorkspaceDao.getGoogleProjectForUserList(Arrays.asList(1L, 2L, 3L)))
        .thenReturn(googleProjectIdsSet);
  }

  private Map<String, Double> getUserCostMap() {
    Map<String, Double> userCostMap = new HashMap<String, Double>();
    userCostMap.put("12", 0.013);
    userCostMap.put("22", 1.123);
    userCostMap.put("23", 6.5);
    userCostMap.put("32", 0.34);
    userCostMap.put("33", 0.9);
    return userCostMap;
  }
}
