package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.v3.model.Project;
import com.google.common.base.Stopwatch;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.initialcredits.InitialCreditsBatchUpdateService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CloudTaskUserControllerTest {
  @Autowired private CloudResourceManagerService mockCloudResourceManagerService;

  private long incrementedUserId = 1L;
  private DbUser userA;
  private DbUser userB;

  @Autowired private CloudTaskUserController controller;

  @MockBean private AccessModuleService mockAccessModuleService;
  @MockBean private InitialCreditsBatchUpdateService mockInitialCreditsBatchUpdateService;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private UserService mockUserService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CloudTaskUserController.class,
    Stopwatch.class,
  })
  @MockBean({
    AccessModuleService.class,
    CloudResourceManagerService.class,
    InitialCreditsBatchUpdateService.class,
    InitialCreditsService.class,
    UserService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    incrementedUserId = 1L;
    userA = createUser("a@fake-research-aou.org");
    userB = createUser("b@fake-research-aou.org");

    when(mockUserService.findUsersById(List.of(userA.getUserId()))).thenReturn(List.of(userA));
    when(mockUserService.findUsersById(List.of(userB.getUserId()))).thenReturn(List.of(userB));
    when(mockUserService.findUsersById(List.of(userA.getUserId(), userB.getUserId())))
        .thenReturn(List.of(userA, userB));
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    return user;
  }

  @Test
  public void testBulkProjectAudit() throws Exception {
    doReturn(Collections.emptyList())
        .when(mockCloudResourceManagerService)
        .getAllProjectsForUser(userA);
    doReturn(List.of(new Project().setName("aou-rw-test-123").setParent("folder/123")))
        .when(mockCloudResourceManagerService)
        .getAllProjectsForUser(userB);
    controller.auditProjectAccessBatch(List.of(userA.getUserId(), userB.getUserId()));
    verify(mockCloudResourceManagerService, times(2)).getAllProjectsForUser(any());
  }

  @Test
  public void testSynchronizeAccess() {
    when(mockAccessModuleService.getAccessModuleStatus(userA, DbAccessModuleName.TWO_FACTOR_AUTH))
        .thenReturn(Optional.of(new AccessModuleStatus().completionEpochMillis(123L)));
    when(mockAccessModuleService.getAccessModuleStatus(userB, DbAccessModuleName.TWO_FACTOR_AUTH))
        .thenReturn(Optional.of(new AccessModuleStatus()));

    // kluge to ensure a valid return value for syncTwoFactorAuthStatus()
    when(mockUserService.syncTwoFactorAuthStatus(userA, Agent.asSystem())).thenReturn(userA);

    controller.synchronizeUserAccessBatch(List.of(userA.getUserId(), userB.getUserId()));

    // Ideally we would use a real implementation of UserService and mock its external deps, but
    // unfortunately UserService is too sprawling to replicate in a unit test.

    verify(mockUserService).findUsersById(List.of(userA.getUserId(), userB.getUserId()));

    // we only sync 2FA users with completed 2FA
    verify(mockUserService).syncTwoFactorAuthStatus(userA, Agent.asSystem());

    // we sync DUCC for all users
    verify(mockUserService).syncDuccVersionStatus(userA, Agent.asSystem());
    verify(mockUserService).syncDuccVersionStatus(userB, Agent.asSystem());

    verifyNoMoreInteractions(mockUserService);
  }

  @Test
  public void testCheckInitialCreditsUsage() {
    List<Long> userIdList = List.of(1L, 2L, 3L);
    controller.checkInitialCreditsUsageBatch(userIdList);
    verify(mockInitialCreditsBatchUpdateService).checkInitialCreditsUsage(userIdList);
  }

  @Test
  public void testCheckInitialCreditsUsage_noUserListPassedFromTask() {
    controller.checkInitialCreditsUsageBatch(Collections.emptyList());
    verify(mockInitialCreditsBatchUpdateService, never()).checkInitialCreditsUsage(any());
  }

  @Test
  public void testCheckInitialCreditsUsage_nullPassedFromTask() {
    controller.checkInitialCreditsUsageBatch(null);
    verify(mockInitialCreditsBatchUpdateService, never()).checkInitialCreditsUsage(any());
  }

  @Test
  public void testCheckCreditsExpirationForUserIDs() {
    List<Long> userIdList = List.of(1L, 2L, 3L);
    controller.checkCreditsExpirationForUserIDsBatch(userIdList);
    verify(mockInitialCreditsService).checkCreditsExpirationForUserIDs(userIdList);
  }
}
