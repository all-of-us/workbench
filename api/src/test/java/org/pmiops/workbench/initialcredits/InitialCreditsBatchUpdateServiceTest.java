package org.pmiops.workbench.initialcredits;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitialCreditsBatchUpdateServiceTest {

  @Autowired private GoogleProjectPerCostDao mockGoogleProjectPerCostDao;
  @Autowired private WorkspaceDao mockWorkspaceDao;
  @Autowired private InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;
  @Autowired private InitialCreditsService mockInitialCreditsService;
  @Autowired private UserDao mockUserDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, InitialCreditsBatchUpdateService.class})
  @MockBean({
    GoogleProjectPerCostDao.class,
    UserDao.class,
    WorkspaceDao.class,
    WorkbenchConfig.class,
    BigQueryService.class,
    InitialCreditsService.class
  })
  static class Configuration {}

  Set<DbUser> mockDbuserSet = new HashSet<DbUser>();

  Set<String> googleProjectIdsSet = new HashSet<>(Arrays.asList("12", "22", "23", "32", "33"));

  @BeforeAll
  public void init() throws Exception {
    mockDbUser();
    mockGoogleProjectsForUser();
    mockGoogleProjectCost();
  }

  @Test
  public void testFreeTierBillingBatchUpdateService() {
    initialCreditsBatchUpdateService.checkAndAlertFreeTierBillingUsage(Arrays.asList(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getGoogleProjectForUserList(Arrays.asList(1L, 2L, 3L));
    verify(mockGoogleProjectPerCostDao, times(1)).findAllByGoogleProjectId(googleProjectIdsSet);

    verify(mockInitialCreditsService)
        .checkFreeTierBillingUsageForUsers(mockDbuserSet, getUserCostMap());
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

  private void mockGoogleProjectCost() {
    List<DbGoogleProjectPerCost> dbGoogleProjectPerCostList =
        Arrays.asList(
            new DbGoogleProjectPerCost("12", 0.013),
            new DbGoogleProjectPerCost("22", 1.123),
            new DbGoogleProjectPerCost("23", 6.5),
            new DbGoogleProjectPerCost("32", 0.34),
            new DbGoogleProjectPerCost("33", 0.9));
    when(mockGoogleProjectPerCostDao.findAllByGoogleProjectId(googleProjectIdsSet))
        .thenReturn(dbGoogleProjectPerCostList);
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
