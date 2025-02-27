package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.GoogleProjectPerCostDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.initialcredits.InitialCreditsBatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class OfflineBillingControllerTest {

  @Autowired private InitialCreditsBatchUpdateService mockInitialCreditsBatchUpdateService;
  @Autowired private OfflineBillingController offlineBillingController;
  @Autowired private TaskQueueService mockTaskQueueService;
  @Autowired private UserService mockUserService;
  @Autowired private GoogleProjectPerCostDao mockGoogleProjectPerCostDao;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, OfflineBillingController.class})
  @MockBean({
    InitialCreditsBatchUpdateService.class,
    TaskQueueService.class,
    UserService.class,
    GoogleProjectPerCostDao.class
  })
  static class Configuration {}

  // Google Project -> Cost
  Map<String, Double> workspaceCosts = Map.of("1", 0.019, "2", 0.4, "3", 1d, "4", 0.34);

  @Test
  public void testCheckInitialCreditsUsage() {
    mockUserId();
    mockInitialCreditCosts();
    offlineBillingController.checkInitialCreditsUsage();

    // Confirm the database is cleared and saved with new value
    verify(mockGoogleProjectPerCostDao).deleteAll();
    verify(mockGoogleProjectPerCostDao).batchInsertProjectPerCost(anyList());

    // Confirm that task as pushed with User Id List
    verify(mockTaskQueueService).groupAndPushInitialCreditsUsage(Arrays.asList(1L, 2L, 3L));
  }

  private void mockUserId() {
    when(mockUserService.getAllUserIds()).thenReturn(Arrays.asList(1L, 2L, 3L));
  }

  private void mockInitialCreditCosts() {
    when(mockInitialCreditsBatchUpdateService.getWorkspaceCostsFromBQ()).thenReturn(workspaceCosts);
  }
}
