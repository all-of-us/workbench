package org.pmiops.workbench.billing;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class FreeTierBillingBatchUpdateServiceTest {

  @Autowired private GoogleProjectPerCostDao mockGoogleProjectPerCostDao;
  @Autowired private WorkspaceDao mockWorkspaceDao;
  @Autowired private FreeTierBillingBatchUpdateService freeTierBillingBatchUpdateService;
  @Autowired private FreeTierBillingService mockFreeTierBillingService;
  @Autowired private UserDao mockUserDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, FreeTierBillingBatchUpdateService.class})
  @MockBean({
    GoogleProjectPerCostDao.class,
    UserDao.class,
    WorkspaceDao.class,
    WorkbenchConfig.class,
    BigQueryService.class,
    FreeTierBillingService.class
  })
  static class Configuration {}

  Set<DbUser> mockDbuserSet = new HashSet<DbUser>();

  @Test
  public void testFreeTierBillingBatchUpdateService() {
    mockDbUser();
    mockGoogleProjectsForUser();
    mockGoogleProjectCost();

    freeTierBillingBatchUpdateService.checkAndAlertFreeTierBillingUsage(
        Arrays.asList(new Long[] {1l, 2l, 3l}));

    verify(mockWorkspaceDao, times(3)).getGoogleProjectForUser(anyLong());
    verify(mockWorkspaceDao).getGoogleProjectForUser(1l);
    verify(mockWorkspaceDao).getGoogleProjectForUser(2l);
    verify(mockWorkspaceDao).getGoogleProjectForUser(3l);
    verify(mockGoogleProjectPerCostDao, times(5)).getCostByGoogleProject(anyString());
    verify(mockGoogleProjectPerCostDao).getCostByGoogleProject("12");
    verify(mockGoogleProjectPerCostDao).getCostByGoogleProject("22");
    verify(mockGoogleProjectPerCostDao).getCostByGoogleProject("23");
    verify(mockGoogleProjectPerCostDao).getCostByGoogleProject("32");
    verify(mockGoogleProjectPerCostDao).getCostByGoogleProject("33");

    verify(mockFreeTierBillingService)
        .checkFreeTierBillingUsageForUsers(mockDbuserSet, getUserCostMap());
  }

  private void mockDbUser() {
    DbUser dbUserForId1 = new DbUser().setUserId(1l);
    DbUser dbUserForId2 = new DbUser().setUserId(2l);
    DbUser dbUserForId3 = new DbUser().setUserId(3l);
    when(mockUserDao.findUserByUserId(1l)).thenReturn(dbUserForId1);
    when(mockUserDao.findUserByUserId(2l)).thenReturn(dbUserForId2);
    when(mockUserDao.findUserByUserId(3l)).thenReturn(dbUserForId3);
    mockDbuserSet.add(dbUserForId1);
    mockDbuserSet.add(dbUserForId2);
    mockDbuserSet.add(dbUserForId3);
  }

  private void mockGoogleProjectsForUser() {
    when(mockWorkspaceDao.getGoogleProjectForUser(1l))
        .thenReturn(new HashSet<>(Arrays.asList("12", "22")));
    when(mockWorkspaceDao.getGoogleProjectForUser(2l))
        .thenReturn(new HashSet<>(Arrays.asList("22", "23")));
    when(mockWorkspaceDao.getGoogleProjectForUser(3l))
        .thenReturn(new HashSet<>(Arrays.asList("32", "33")));
  }

  private void mockGoogleProjectCost() {
    when(mockGoogleProjectPerCostDao.getCostByGoogleProject("12")).thenReturn(0.013);
    when(mockGoogleProjectPerCostDao.getCostByGoogleProject("22")).thenReturn(1.123);
    when(mockGoogleProjectPerCostDao.getCostByGoogleProject("23")).thenReturn(6.5);
    when(mockGoogleProjectPerCostDao.getCostByGoogleProject("32")).thenReturn(0.34);
    when(mockGoogleProjectPerCostDao.getCostByGoogleProject("33")).thenReturn(0.9);
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
