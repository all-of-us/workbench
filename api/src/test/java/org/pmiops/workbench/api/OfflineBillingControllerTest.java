package org.pmiops.workbench.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.model.UserBQCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class OfflineBillingControllerTest {

  @Autowired private FreeTierBillingBatchUpdateService mockFreeTierBillingUpdateService;
  @Autowired private OfflineBillingController offlineBillingController;
  @Autowired private TaskQueueService mockTaskQueueService;
  @Autowired private UserService mockUserService;
  @Autowired private WorkspaceDao mockWorkspaceDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, OfflineBillingController.class})
  @MockBean({
    FreeTierBillingBatchUpdateService.class,
    TaskQueueService.class,
    UserService.class,
    WorkspaceDao.class,
  })
  static class Configuration {}

  Map<String, Double> freeTierForAllWorkspace = new HashMap<>();

  @Test
  public void testCheckFreeTierBillingUsage() {
    offlineBillingController.checkFreeTierBillingUsage();
    verify(mockFreeTierBillingUpdateService).checkFreeTierBillingUsage();
  }

  @Test
  public void testCheckFreeTierBillingUsageCloudTask() {
    mockUserId();
    mockFreeTierCostForGP();
    mockUserGPLink();
    offlineBillingController.checkFreeTierBillingUsageCloudTask();

    verify(mockTaskQueueService).groupAndPushFreeTierBilling(getUserBQCostList());
  }

  private void mockFreeTierCostForGP() {
    // Key: Google Project Value: Cost
    freeTierForAllWorkspace.put("1", 0.019);
    freeTierForAllWorkspace.put("2", 0.4);
    freeTierForAllWorkspace.put("3", 1d);
    freeTierForAllWorkspace.put("4", 0.34);
    when(mockFreeTierBillingUpdateService.getFreeTierWorkspaceCostsFromBQ())
        .thenReturn(freeTierForAllWorkspace);
  }

  private void mockUserId() {
    when(mockUserService.getAllUserIds()).thenReturn(Arrays.asList(new Long[] {1l, 2l, 3l}));
  }

  private void mockUserGPLink() {
    Set<String> googleProjectsForUserId1 =
        new HashSet<String>() {
          {
            add("1");
            add("3");
            add("5");
          }
        };

    when(mockWorkspaceDao.getGoogleProjectForUser(1l)).thenReturn(googleProjectsForUserId1);
    Set<String> googleProjectsForUserId2 =
        new HashSet<String>() {
          {
            add("2");
            add("3");
          }
        };

    when(mockWorkspaceDao.getGoogleProjectForUser(2l)).thenReturn(googleProjectsForUserId2);
    when(mockWorkspaceDao.getGoogleProjectForUser(3l)).thenReturn(new HashSet<String>());
  }

  private List getUserBQCostList() {
    UserBQCost userBQCostForId1 = new UserBQCost();
    UserBQCost userBQCostForId2 = new UserBQCost();
    UserBQCost userBQCostForId3 = new UserBQCost();

    // Check mockWorkspaceCost() userId 1 has with googleProject 1,3 and 5
    userBQCostForId1.userId(1l);
    Map freeTierForUserId1 =
        new HashMap() {
          {
            put("1", freeTierForAllWorkspace.get("1"));
            put("3", freeTierForAllWorkspace.get("3"));
          }
        };

    userBQCostForId1.workspaceBQCost(freeTierForUserId1);

    // Check mockWorkspaceCost() userId 2 has with googleProject 2 and 3
    userBQCostForId2.userId(2l);
    Map freeTierForUserId2 =
        new HashMap() {
          {
            put("2", freeTierForAllWorkspace.get("2"));
            put("3", freeTierForAllWorkspace.get("3"));
          }
        };
    userBQCostForId2.workspaceBQCost(freeTierForUserId2);

    // Check mockWorkspaceCost() userId 3 has no google Project
    userBQCostForId3.userId(3l);
    userBQCostForId3.workspaceBQCost(new HashMap<>());

    List allUserCost = new ArrayList();
    allUserCost.add(userBQCostForId1);
    allUserCost.add(userBQCostForId2);
    allUserCost.add(userBQCostForId3);
    return allUserCost;
  }
}
