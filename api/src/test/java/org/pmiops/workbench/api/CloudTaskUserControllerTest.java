package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudresourcemanager.v3.model.Project;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.pmiops.workbench.model.SynchronizeUserAccessRequest;
import org.pmiops.workbench.model.UserListRequest;
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
  @Autowired private UserDao userDao;

  @Autowired private AccessModuleService mockAccessModuleService;
  @Autowired private UserService mockUserService;

  @Autowired private FreeTierBillingBatchUpdateService mockFreeTierBillingUpdateService;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, CloudTaskUserController.class})
  @MockBean({
    AccessModuleService.class,
    CloudResourceManagerService.class,
    FreeTierBillingBatchUpdateService.class,
    UserService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    incrementedUserId = 1L;
    userA = createUser("a@fake-research-aou.org");
    userB = createUser("b@fake-research-aou.org");
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    return userDao.save(user);
  }

  @Test
  public void testBulkProjectAudit() throws Exception {
    doReturn(ImmutableList.of()).when(mockCloudResourceManagerService).getAllProjectsForUser(userA);
    doReturn(ImmutableList.of(new Project().setName("aou-rw-test-123").setParent("folder/123")))
        .when(mockCloudResourceManagerService)
        .getAllProjectsForUser(userB);
    controller.auditProjectAccess(
        new AuditProjectAccessRequest()
            .addUserIdsItem(userA.getUserId())
            .addUserIdsItem(userB.getUserId()));
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

    controller.synchronizeUserAccess(
        new SynchronizeUserAccessRequest()
            .addUserIdsItem(userA.getUserId())
            .addUserIdsItem(userB.getUserId()));

    // Ideally we would use a real implementation of UserService and mock its external deps, but
    // unfortunately UserService is too sprawling to replicate in a unit test.

    // we only sync 2FA users with completed 2FA
    verify(mockUserService).syncTwoFactorAuthStatus(userA, Agent.asSystem());

    // we sync DUCC for all users
    verify(mockUserService).syncDuccVersionStatus(userA, Agent.asSystem());
    verify(mockUserService).syncDuccVersionStatus(userB, Agent.asSystem());

    verifyNoMoreInteractions(mockUserService);
  }

  @Test
  public void testCheckAndAlertFreeTierBillingUsage() {
    long[] userIds = {1, 2, 3};
    List userIdsList = Arrays.asList(userIds);
    UserListRequest userListRequest = new UserListRequest();
    userListRequest.userIds(userIdsList);
    controller.checkAndAlertFreeTierBillingUsage(userListRequest);
    verify(mockFreeTierBillingUpdateService).checkAndAlertFreeTierBillingUsage(userIdsList);
  }
}
