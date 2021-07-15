package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Functions;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.DirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OfflineUserControllerTest extends SpringTest {
  @Autowired private UserService mockUserService;
  @Autowired private DirectoryService mockDirectoryService;
  @Autowired private OfflineUserController offlineUserController;

  private Long incrementedUserId = 1L;

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({OfflineUserController.class})
  @MockBean({
    AccessTierService.class,
    DirectoryService.class,
    TaskQueueService.class,
    UserService.class
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    when(mockUserService.getAllUsersExcludingDisabled()).thenReturn(getUsers());
    when(mockUserService.getAllUsers()).thenReturn(getUsers());
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
  }

  private DbUser createUser(String email) {
    return createUser(email, true);
  }

  private DbUser createUser(String email, boolean signedIn) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    if (signedIn) {
      user.setFirstSignInTime(Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z")));
    }
    incrementedUserId++;
    return user;
  }

  private List<DbUser> getUsers() {
    return Arrays.asList(
        createUser("a@fake-research-aou.org"),
        createUser("b@fake-research-aou.org"),
        createUser("c@fake-research-aou.org"),
        createUser("never-signed-in@fake-research-aou.org", false));
  }

  @Test
  public void testBulkSyncTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    // Mock out the service under test to simply return the passed user argument.
    doAnswer(i -> i.getArgument(0))
        .when(mockUserService)
        .syncComplianceTrainingStatusV2(any(), any());
    offlineUserController.bulkSyncComplianceTrainingStatus();
    verify(mockUserService, times(4)).syncComplianceTrainingStatusV2(any(), any());
  }

  @Test
  public void testBulkSyncTrainingStatusWithSingleUserErrorV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    assertThrows(
        ServerErrorException.class,
        () -> {
          doAnswer(i -> i.getArgument(0))
              .when(mockUserService)
              .syncComplianceTrainingStatusV2(any(), any());
          doThrow(new org.pmiops.workbench.moodle.ApiException("Unknown error"))
              .when(mockUserService)
              .syncComplianceTrainingStatusV2(
                  argThat(user -> user.getUsername().equals("a@fake-research-aou.org")), any());
          offlineUserController.bulkSyncComplianceTrainingStatus();
          // Even when a single call throws an exception, we call the service for all users.
          verify(mockUserService, times(4)).syncComplianceTrainingStatusV2(any(), any());
        });
  }

  @Test
  public void testBulkSyncTwoFactorAuthSync() {
    Map<String, Boolean> allTwoFactorEnabled =
        getUsers().stream()
            .collect(Collectors.toMap(DbUser::getUsername, Functions.constant(true)));
    doReturn(allTwoFactorEnabled).when(mockDirectoryService).getAllTwoFactorAuthStatuses();
    doAnswer(i -> i.getArgument(0))
        .when(mockUserService)
        .syncTwoFactorAuthStatus(any(), any(), anyBoolean());

    offlineUserController.bulkSyncTwoFactorAuthStatus();
    verify(mockUserService, times(4)).syncTwoFactorAuthStatus(any(), any(), eq(true));
  }

  @Test
  public void testBulkSyncTwoFactorAuthSyncMissingUsers() {
    Map<String, Boolean> allTwoFactorEnabled =
        getUsers().stream()
            .limit(2)
            .collect(Collectors.toMap(DbUser::getUsername, Functions.constant(false)));
    doReturn(allTwoFactorEnabled).when(mockDirectoryService).getAllTwoFactorAuthStatuses();
    doAnswer(i -> i.getArgument(0))
        .when(mockUserService)
        .syncTwoFactorAuthStatus(any(), any(), anyBoolean());

    try {
      offlineUserController.bulkSyncTwoFactorAuthStatus();
    } catch (ServerErrorException e) {
      // expected
    }
    verify(mockUserService, times(2)).syncTwoFactorAuthStatus(any(), any(), eq(false));
  }

  @Test
  public void testBulkSyncEraCommonsStatus()
      throws IOException, org.pmiops.workbench.firecloud.ApiException {
    doAnswer(i -> i.getArgument(0))
        .when(mockUserService)
        .syncEraCommonsStatusUsingImpersonation(any(), any());
    offlineUserController.bulkSyncEraCommonsStatus();
    verify(mockUserService, times(3)).syncEraCommonsStatusUsingImpersonation(any(), any());
  }

  @Test
  public void testBulkSyncEraCommonsStatusWithSingleUserError()
      throws ApiException, NotFoundException, IOException,
          org.pmiops.workbench.firecloud.ApiException {
    assertThrows(
        ServerErrorException.class,
        () -> {
          doAnswer(i -> i.getArgument(0))
              .when(mockUserService)
              .syncEraCommonsStatusUsingImpersonation(any(), any());
          doThrow(new org.pmiops.workbench.firecloud.ApiException("Unknown error"))
              .when(mockUserService)
              .syncEraCommonsStatusUsingImpersonation(
                  argThat(user -> user.getUsername().equals("a@fake-research-aou.org")), any());
          offlineUserController.bulkSyncEraCommonsStatus();
          // Even when a single call throws an exception, we call the service for all users.
          verify(mockUserService, times(3)).syncEraCommonsStatusUsingImpersonation(any(), any());
        });
  }
}
