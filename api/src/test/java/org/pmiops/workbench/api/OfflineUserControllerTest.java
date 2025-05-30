package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.cloudtasks.TaskQueueService.AUDIT_PROJECTS;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.Task;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
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
public class OfflineUserControllerTest {

  @Autowired private UserService mockUserService;
  @Autowired private OfflineUserController offlineUserController;
  @Autowired private CloudTasksClient mockCloudTasksClient;

  private Long incrementedUserId = 1L;

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    OfflineUserController.class,
    Stopwatch.class,
    TaskQueueService.class,
    WorkbenchLocationConfigService.class,
  })
  @MockBean({CloudTasksClient.class, UserService.class})
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    incrementedUserId = 1L;
    List<DbUser> users = createUsers();
    List<Long> userIds = users.stream().map(DbUser::getUserId).toList();
    when(mockUserService.getAllUserIds()).thenReturn(userIds);
    when(mockUserService.getAllUserIdsWithActiveInitialCredits()).thenReturn(userIds);
    when(mockUserService.getAllUserIdsWithCurrentTierAccess()).thenReturn(userIds);
    when(mockUserService.getAllUsers()).thenReturn(users);
    when(mockCloudTasksClient.createTask(anyString(), any(Task.class)))
        .thenReturn(Task.newBuilder().setName("name").build());

    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.projectId = "test";
    workbenchConfig.server.appEngineLocationId = "us-central";
    workbenchConfig.offlineBatch.usersPerCheckInitialCreditsExpirationTask = 2;
    workbenchConfig.offlineBatch.usersPerAuditTask = 2;
    workbenchConfig.offlineBatch.usersPerSynchronizeAccessTask = 3;
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

  private List<DbUser> createUsers() {
    return Arrays.asList(
        createUser("a@fake-research-aou.org"),
        createUser("b@fake-research-aou.org"),
        createUser("c@fake-research-aou.org"),
        createUser("never-signed-in@fake-research-aou.org", false));
  }

  @Test
  public void testSynchronizeAccessTasks() {
    offlineUserController.synchronizeUserAccess();

    // Batch size is 3, so we expect 2 groups.
    for (List<Long> expected : List.of(List.of(1L, 2L, 3L), List.of(4L))) {
      verify(mockCloudTasksClient)
          .createTask(
              matches(TaskQueueService.SYNCHRONIZE_ACCESS.queueName()),
              argThat(taskRequest -> expected.equals(cloudTaskToUserIdList(taskRequest))));
    }
    verifyNoMoreInteractions(mockCloudTasksClient);
  }

  @Test
  public void testBulkAuditProjectAccess() {
    offlineUserController.bulkAuditProjectAccess();

    // Batch size is 2, so we expect 2 groups.
    for (List<Long> expected : List.of(List.of(1L, 2L), List.of(3L, 4L))) {
      verify(mockCloudTasksClient)
          .createTask(
              matches(AUDIT_PROJECTS.queueName()),
              argThat(taskRequest -> expected.equals(cloudTaskToUserIdList(taskRequest))));
    }
    verifyNoMoreInteractions(mockCloudTasksClient);
  }

  @Test
  public void testCheckInitialCreditsExpiration() {
    offlineUserController.checkInitialCreditsExpiration();

    // Batch size is 2, so we expect 2 groups.
    for (List<Long> expected : List.of(List.of(1L, 2L), List.of(3L, 4L))) {
      verify(mockCloudTasksClient)
          .createTask(
              matches(TaskQueueService.INITIAL_CREDITS_EXPIRATION.queueName()),
              argThat(taskRequest -> expected.equals(cloudTaskToUserIdList(taskRequest))));
    }
    verifyNoMoreInteractions(mockCloudTasksClient);
  }

  private List<Long> cloudTaskToUserIdList(Task t) {
    Type listType = new TypeToken<List<Long>>() {}.getType();
    return new Gson().fromJson(t.getAppEngineHttpRequest().getBody().toStringUtf8(), listType);
  }
}
