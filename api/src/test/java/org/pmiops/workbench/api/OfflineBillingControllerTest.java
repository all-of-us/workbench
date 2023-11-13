package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserService;
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
  @Autowired private GoogleProjectPerCostDao mockGoogleProjectPerCostDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, OfflineBillingController.class})
  @MockBean({
    FreeTierBillingBatchUpdateService.class,
    TaskQueueService.class,
    UserService.class,
    GoogleProjectPerCostDao.class
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
    offlineBillingController.checkFreeTierBillingUsageCloudTask();

    // Confirm the database is cleared and saved with new value
    verify(mockGoogleProjectPerCostDao).deleteAll();
    verify(mockGoogleProjectPerCostDao).saveAll(anyList());

    // Confirm that task as pushed with User Id List
    verify(mockTaskQueueService)
        .groupAndPushFreeTierBilling(Arrays.asList(new Long[] {1l, 2l, 3l}));
  }

  private void mockUserId() {
    when(mockUserService.getAllUserIds()).thenReturn(Arrays.asList(new Long[] {1l, 2l, 3l}));
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
}
