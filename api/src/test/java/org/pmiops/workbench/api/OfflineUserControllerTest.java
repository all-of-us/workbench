package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.model.Project;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OfflineUserControllerTest {
  @Autowired private CloudResourceManagerService cloudResourceManagerService;
  @Autowired private UserService userService;
  @Autowired private OfflineUserController offlineUserController;

  private Long incrementedUserId = 1L;

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({OfflineUserController.class})
  @MockBean({CloudResourceManagerService.class, UserService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    when(userService.getAllUsers()).thenReturn(getUsers());
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    return user;
  }

  private List<DbUser> getUsers() {
    return Arrays.asList(
        createUser("a@fake-research-aou.org"),
        createUser("b@fake-research-aou.org"),
        createUser("c@fake-research-aou.org"));
  }

  @Test
  public void testBulkSyncTrainingStatusV1()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    workbenchConfig.featureFlags.enableMoodleV2Api = false;
    // Mock out the service under test to simply return the passed user argument.
    doAnswer(i -> i.getArgument(0)).when(userService).syncComplianceTrainingStatusV1(any(), any());
    offlineUserController.bulkSyncComplianceTrainingStatus();
    verify(userService, times(3)).syncComplianceTrainingStatusV1(any(), any());
  }

  @Test
  public void testBulkSyncTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    workbenchConfig.featureFlags.enableMoodleV2Api = true;
    // Mock out the service under test to simply return the passed user argument.
    doAnswer(i -> i.getArgument(0)).when(userService).syncComplianceTrainingStatusV2(any(), any());
    offlineUserController.bulkSyncComplianceTrainingStatus();
    verify(userService, times(3)).syncComplianceTrainingStatusV2(any(), any());
  }

  @Test(expected = ServerErrorException.class)
  public void testBulkSyncTrainingStatusWithSingleUserErrorV1()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    workbenchConfig.featureFlags.enableMoodleV2Api = false;
    doAnswer(i -> i.getArgument(0)).when(userService).syncComplianceTrainingStatusV1(any(), any());
    doThrow(new org.pmiops.workbench.moodle.ApiException("Unknown error"))
        .when(userService)
        .syncComplianceTrainingStatusV1(
            argThat(user -> user.getUsername().equals("a@fake-research-aou.org")), any());
    offlineUserController.bulkSyncComplianceTrainingStatus();
    // Even when a single call throws an exception, we call the service for all users.
    verify(userService, times(3)).syncComplianceTrainingStatusV1(any(), any());
  }

  @Test(expected = ServerErrorException.class)
  public void testBulkSyncTrainingStatusWithSingleUserErrorV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    workbenchConfig.featureFlags.enableMoodleV2Api = true;
    doAnswer(i -> i.getArgument(0)).when(userService).syncComplianceTrainingStatusV2(any(), any());
    doThrow(new org.pmiops.workbench.moodle.ApiException("Unknown error"))
        .when(userService)
        .syncComplianceTrainingStatusV2(
            argThat(user -> user.getUsername().equals("a@fake-research-aou.org")), any());
    offlineUserController.bulkSyncComplianceTrainingStatus();
    // Even when a single call throws an exception, we call the service for all users.
    verify(userService, times(3)).syncComplianceTrainingStatusV2(any(), any());
  }

  @Test
  public void testBulkSyncEraCommonsStatus()
      throws IOException, org.pmiops.workbench.firecloud.ApiException {
    doAnswer(i -> i.getArgument(0))
        .when(userService)
        .syncEraCommonsStatusUsingImpersonation(any(), any());
    offlineUserController.bulkSyncEraCommonsStatus();
    verify(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any(), any());
  }

  @Test(expected = ServerErrorException.class)
  public void testBulkSyncEraCommonsStatusWithSingleUserError()
      throws ApiException, NotFoundException, IOException,
          org.pmiops.workbench.firecloud.ApiException {
    doAnswer(i -> i.getArgument(0))
        .when(userService)
        .syncEraCommonsStatusUsingImpersonation(any(), any());
    doThrow(new org.pmiops.workbench.firecloud.ApiException("Unknown error"))
        .when(userService)
        .syncEraCommonsStatusUsingImpersonation(
            argThat(user -> user.getUsername().equals("a@fake-research-aou.org")), any());
    offlineUserController.bulkSyncEraCommonsStatus();
    // Even when a single call throws an exception, we call the service for all users.
    verify(userService, times(3)).syncEraCommonsStatusUsingImpersonation(any(), any());
  }

  @Test
  public void testBulkProjectAudit() {
    List<Project> projectList = new ArrayList<>();
    doReturn(projectList).when(cloudResourceManagerService).getAllProjectsForUser(any());
    offlineUserController.bulkAuditProjectAccess();
    verify(cloudResourceManagerService, times(3)).getAllProjectsForUser(any());
  }
}
