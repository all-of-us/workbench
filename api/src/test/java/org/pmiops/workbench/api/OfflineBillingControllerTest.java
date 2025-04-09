package org.pmiops.workbench.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class OfflineBillingControllerTest {

  @Autowired private OfflineBillingController offlineBillingController;

  @MockBean private TaskQueueService mockTaskQueueService;
  @MockBean private UserService mockUserService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    OfflineBillingController.class,
  })
  static class Configuration {}

  @Test
  public void testCheckInitialCreditsUsage() {
    mockUserId();
    offlineBillingController.checkInitialCreditsUsage();

    // Confirm that task as pushed with User Id List
    verify(mockTaskQueueService).groupAndPushInitialCreditsUsage(Arrays.asList(1L, 2L, 3L));
  }

  private void mockUserId() {
    when(mockUserService.getAllUserIds()).thenReturn(Arrays.asList(1L, 2L, 3L));
  }
}
