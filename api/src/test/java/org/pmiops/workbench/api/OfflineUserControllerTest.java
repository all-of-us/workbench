package org.pmiops.workbench.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OfflineUserControllerTest {

  @TestConfiguration
  @Import({
      OfflineUserController.class
  })
  static class Configuration {
    @MockBean
    private UserService userService;
  }

  @Autowired
  private UserService userService;
  @Autowired
  private OfflineUserController offlineUserController;

  private Long incrementedUserId = 1L;

  @Before
  public void setUp() {
    when(userService.getAllUsers()).thenReturn(getUsers());
  }

  private User createUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    return user;
  }

  private List<User> getUsers() {
    return Arrays.asList(
        createUser("a@fake-research-aou.org"),
        createUser("b@fake-research-aou.org"),
        createUser("c@fake-research-aou.org"));
  }

  @Test
  public void testBulkSyncTrainingStatus() throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    offlineUserController.bulkSyncTrainingStatus();
    verify(userService, times(3)).syncComplianceTrainingStatus(any());
  }

  @Test
  public void testBulkSyncTrainingStatusWithSingleUserError() throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    doThrow(new org.pmiops.workbench.moodle.ApiException("Unknown error"))
        .when(userService).syncComplianceTrainingStatus(argThat(user -> user.getEmail() == "a@fake-research-aou.org"));
    offlineUserController.bulkSyncTrainingStatus();
    // Even when a single call throws an exception, we call the service for all users.
    verify(userService, times(3)).syncComplianceTrainingStatus(any());
  }

  @Test
  public void testBulkSyncEraCommonsStatus() throws IOException, org.pmiops.workbench.firecloud.ApiException {
    offlineUserController.bulkSyncEraCommonsStatus();
    verify(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any());
  }

  @Test
  public void testBulkSyncEraCommonsStatusWithSingleUserError() throws ApiException, NotFoundException, IOException, org.pmiops.workbench.firecloud.ApiException {
    doThrow(new org.pmiops.workbench.firecloud.ApiException("Unknown error"))
        .when(userService).syncEraCommonsStatusUsingImpersonation(argThat(user -> user.getEmail() == "a@fake-research-aou.org"));
    offlineUserController.bulkSyncEraCommonsStatus();
    // Even when a single call throws an exception, we call the service for all users.
    verify(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any());
  }

}
