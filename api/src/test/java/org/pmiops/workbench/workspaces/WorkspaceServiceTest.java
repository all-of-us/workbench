package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.exfiltration.ExfiltrationConstants.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTierCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.profile.ProfileMapper;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class WorkspaceServiceTest {

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class,
    WorkspaceAuthService.class,
    CloudStorageClientImpl.class,
    ObjectNameLengthServiceImpl.class,
  })
  @MockBean({
    AccessTierService.class,
    BillingProjectAuditor.class,
    CohortCloningService.class,
    CohortService.class,
    ConceptSetService.class,
    DataSetService.class,
    FirecloudMapper.class,
    FreeTierBillingService.class,
    ProfileMapper.class,
    UserDao.class,
    UserMapper.class,
    IamService.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }
  }

  @MockBean private BillingProjectAuditor mockBillingProjectAuditor;
  @MockBean private Clock mockClock;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private CloudBillingClient mockCloudBillingClient;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;
  @MockBean private AccessTierService accessTierService;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @MockBean private BucketAuditQueryService bucketAuditQueryService;
  @MockBean UserService userService;

  @MockBean
  @Qualifier(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
  EgressRemediationService egressRemediationService;

  private static DbUser currentUser;

  private final List<FirecloudWorkspaceResponse> firecloudWorkspaceResponses = new ArrayList<>();
  private final List<DbWorkspace> dbWorkspaces = new ArrayList<>();
  private static final Instant NOW = Instant.parse("1985-11-05T22:04:00.00Z");
  private static final long USER_ID = 1L;
  private static final String DEFAULT_USERNAME = "mock@mock.com";
  private static final String DEFAULT_WORKSPACE_NAMESPACE = "namespace";
  private static final String FREE_TIER_BILLING_ACCOUNT_ID = "free-tier-account";

  private final AtomicLong workspaceIdIncrementer = new AtomicLong(1);

  private static WorkbenchConfig workbenchConfig;

  @BeforeEach
  public void setUp() {
    doReturn(NOW).when(mockClock).instant();

    firecloudWorkspaceResponses.clear();
    dbWorkspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(firecloudWorkspaceResponses).when(mockFireCloudService).getWorkspaces();

    currentUser = new DbUser();
    currentUser.setUsername(DEFAULT_USERNAME);
    currentUser.setUserId(USER_ID);
    currentUser.setDisabled(false);

    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.accountId = FREE_TIER_BILLING_ACCOUNT_ID;
  }

  private FirecloudWorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel) {
    FirecloudWorkspaceDetails mockWorkspace = mock(FirecloudWorkspaceDetails.class);
    doReturn(workspaceNamespace).when(mockWorkspace).getNamespace();
    doReturn(workspaceName).when(mockWorkspace).getName();
    doReturn(workspaceId).when(mockWorkspace).getWorkspaceId();

    FirecloudWorkspaceResponse mockWorkspaceResponse = mock(FirecloudWorkspaceResponse.class);
    doReturn(mockWorkspace).when(mockWorkspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(mockWorkspaceResponse).getAccessLevel();
    return mockWorkspaceResponse;
  }

  private DbWorkspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    DbWorkspace workspace = new DbWorkspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(dbId);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(dbId));
    workspace.setNeedsReviewPrompt(false);
    return workspace;
  }

  private DbWorkspace addMockedWorkspace(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    FirecloudWorkspaceResponse mockWorkspaceResponse =
        mockFirecloudWorkspaceResponse(workspaceId, workspaceName, workspaceNamespace, accessLevel);

    DbWorkspace dbWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                workspaceId,
                mockWorkspaceResponse.getWorkspace().getName(),
                workspaceNamespace,
                activeStatus));

    dbWorkspaces.add(dbWorkspace);
    return dbWorkspace;
  }

  private DbWorkspace addMockedWorkspace(DbWorkspace dbWorkspace) {

    mockFirecloudWorkspaceResponse(
        dbWorkspace.getWorkspaceId(),
        dbWorkspace.getName(),
        dbWorkspace.getWorkspaceNamespace(),
        WorkspaceAccessLevel.OWNER);

    workspaceDao.save(dbWorkspace);
    dbWorkspaces.add(dbWorkspace);
    return dbWorkspace;
  }

  private FirecloudWorkspaceResponse mockFirecloudWorkspaceResponse(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel) {
    FirecloudWorkspaceResponse mockWorkspaceResponse =
        mockFirecloudWorkspaceResponse(
            Long.toString(workspaceId), workspaceName, workspaceNamespace, accessLevel);
    firecloudWorkspaceResponses.add(mockWorkspaceResponse);
    doReturn(mockWorkspaceResponse)
        .when(mockFireCloudService)
        .getWorkspace(workspaceNamespace, workspaceName);
    return mockWorkspaceResponse;
  }

  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(5);
  }

  @Test
  public void getWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "deleted",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void activeStatus() {
    EnumSet.allOf(WorkspaceActiveStatus.class)
        .forEach(
            status ->
                assertThat(
                        buildDbWorkspace(
                                workspaceIdIncrementer.getAndIncrement(),
                                "1",
                                DEFAULT_WORKSPACE_NAMESPACE,
                                status)
                            .getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }

  @Test
  public void updateRecentWorkspaces() {
    dbWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          doReturn(NOW.minusSeconds(dbWorkspaces.size() - workspace.getWorkspaceId()))
              .when(mockClock)
              .instant();
          workspaceService.updateRecentWorkspaces(workspace);
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT);

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        dbWorkspaces
            .subList(
                dbWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                dbWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsExactlyElementsIn(expectedIds);
  }

  @Test
  public void updateRecentWorkspaces_multipleUsers() {
    long OTHER_USER_ID = 2L;
    currentUser.setUserId(OTHER_USER_ID);
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(0));

    currentUser.setUserId(USER_ID);
    dbWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          doReturn(NOW.minusSeconds(dbWorkspaces.size() - workspace.getWorkspaceId()))
              .when(mockClock)
              .instant();
          workspaceService.updateRecentWorkspaces(workspace);
        });
    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();

    assertThat(recentWorkspaces.size()).isEqualTo(4);
    recentWorkspaces.forEach(
        userRecentWorkspace ->
            assertThat(userRecentWorkspace.getId())
                .isNotEqualTo(userRecentWorkspace.getWorkspaceId()));

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        dbWorkspaces
            .subList(
                dbWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                dbWorkspaces.size())
            .stream()
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsExactlyElementsIn(expectedIds);

    currentUser.setUsername(DEFAULT_USERNAME);
    currentUser.setUserId(OTHER_USER_ID);

    final List<DbUserRecentWorkspace> otherRecentWorkspaces =
        workspaceService.getRecentWorkspaces();
    assertThat(otherRecentWorkspaces.size()).isEqualTo(1);
    assertThat(otherRecentWorkspaces.get(0).getWorkspaceId())
        .isEqualTo(dbWorkspaces.get(0).getWorkspaceId());
  }

  @Test
  public void updateRecentWorkspaces_flipFlop() {
    doReturn(NOW.minusSeconds(4)).when(mockClock).instant();
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(0));

    doReturn(NOW.minusSeconds(3)).when(mockClock).instant();
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(1));

    doReturn(NOW.minusSeconds(2)).when(mockClock).instant();
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(0));

    doReturn(NOW.minusSeconds(1)).when(mockClock).instant();
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(1));

    doReturn(NOW).when(mockClock).instant();
    workspaceService.updateRecentWorkspaces(dbWorkspaces.get(0));

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(2);
    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsExactly(1L, 2L);
  }

  @Test
  public void enforceFirecloudAclsInRecentWorkspaces() {
    long ownedId = workspaceIdIncrementer.getAndIncrement();
    DbWorkspace ownedWorkspace =
        addMockedWorkspace(
            ownedId,
            "owned",
            "owned_namespace",
            WorkspaceAccessLevel.OWNER,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(ownedWorkspace);

    DbWorkspace sharedWorkspace =
        addMockedWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "shared",
            "shared_namespace",
            WorkspaceAccessLevel.NO_ACCESS,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(sharedWorkspace);

    List<DbUserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(1);
    assertThat(recentWorkspaces.get(0).getWorkspaceId()).isEqualTo(ownedId);
  }

  @Test
  public void deleteWorkspace() {
    DbWorkspace ws = dbWorkspaces.get(0); // arbitrary choice of those defined for testing
    workspaceService.deleteWorkspace(ws);
    assertThat(ws.getWorkspaceActiveStatusEnum()).isEqualTo(WorkspaceActiveStatus.DELETED);

    String billingProject = ws.getWorkspaceNamespace();
    verify(mockFireCloudService).deleteWorkspace(eq(billingProject), eq(ws.getName()));
    verify(mockFireCloudService).deleteBillingProject(eq(billingProject));
    verify(mockBillingProjectAuditor).fireDeleteAction(eq(billingProject));
  }

  @Test
  public void deleteWorkspace_failedProjectDeletion() {
    DbWorkspace ws = dbWorkspaces.get(1); // arbitrary choice of those defined for testing
    String billingProject = ws.getWorkspaceNamespace();

    doThrow(new BadRequestException("arbitrary"))
        .when(mockFireCloudService)
        .deleteBillingProject(anyString());

    workspaceService.deleteWorkspace(ws);

    // deletion succeeds

    assertThat(ws.getWorkspaceActiveStatusEnum()).isEqualTo(WorkspaceActiveStatus.DELETED);
    verify(mockFireCloudService).deleteWorkspace(eq(billingProject), eq(ws.getName()));
    verify(mockFireCloudService).deleteBillingProject(eq(billingProject));

    // but the billing project is not deleted

    verify(mockBillingProjectAuditor, never()).fireDeleteAction(eq(billingProject));
  }

  @Test
  public void updateBillingAccount_freeTierToUserOwned() throws Exception {
    String newBillingAccount = "billing-123";
    DbWorkspace workspace = dbWorkspaces.get(1); // arbitrary choice of those defined for testing
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
    ProjectBillingInfo projectBillingInfo =
        new ProjectBillingInfo()
            .setProjectId(workspace.getGoogleProject())
            .setBillingAccountName(newBillingAccount)
            .setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(
            workspace.getGoogleProject(), newBillingAccount))
        .thenReturn(projectBillingInfo);

    assertThat(workspace.getBillingAccountName())
        .isEqualTo(workbenchConfig.billing.freeTierBillingAccountName());

    workspaceService.updateWorkspaceBillingAccount(workspace, newBillingAccount);

    verify(mockFireCloudService)
        .updateBillingAccount(workspace.getWorkspaceNamespace(), newBillingAccount);
    verify(mockFireCloudService, never()).updateBillingAccountAsService(anyString(), anyString());
    assertThat(workspace.getBillingAccountName()).isEqualTo(newBillingAccount);
  }

  @Test
  public void updateBillingAccount_userOwnedToFreeTier() throws Exception {
    String oldBillingAccount = "billing-123";
    DbWorkspace workspace = dbWorkspaces.get(1); // arbitrary choice of those defined for testing
    workspace.setBillingAccountName(oldBillingAccount);

    assertThat(workspace.getBillingAccountName()).isEqualTo(oldBillingAccount);

    ProjectBillingInfo projectBillingInfo =
        new ProjectBillingInfo()
            .setProjectId(workspace.getGoogleProject())
            .setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName())
            .setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(
            workspace.getGoogleProject(), workbenchConfig.billing.freeTierBillingAccountName()))
        .thenReturn(projectBillingInfo);
    workspaceService.updateWorkspaceBillingAccount(
        workspace, workbenchConfig.billing.freeTierBillingAccountName());

    verify(mockFireCloudService)
        .updateBillingAccountAsService(
            workspace.getWorkspaceNamespace(),
            workbenchConfig.billing.freeTierBillingAccountName());
    verify(mockFireCloudService, never()).updateBillingAccount(anyString(), anyString());
    assertThat(workspace.getBillingAccountName())
        .isEqualTo(workbenchConfig.billing.freeTierBillingAccountName());
  }

  @Test
  public void updateBillingAccount_noChange() throws Exception {
    String newBillingAccount = "billing-123";
    DbWorkspace workspace = dbWorkspaces.get(1); // arbitrary choice of those defined for testing
    workspace.setBillingAccountName(newBillingAccount);

    workspaceService.updateWorkspaceBillingAccount(workspace, newBillingAccount);

    verify(mockFireCloudService, never()).updateBillingAccountAsService(anyString(), anyString());
    verify(mockFireCloudService, never()).updateBillingAccount(anyString(), anyString());
  }

  @Test
  public void updateBillingAccount_accountNotOpen() throws Exception {
    String newBillingAccount = "billing-123";
    DbWorkspace workspace = dbWorkspaces.get(1); // arbitrary choice of those defined for testing
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());

    ProjectBillingInfo projectBillingInfo =
        new ProjectBillingInfo()
            .setProjectId(workspace.getGoogleProject())
            .setBillingAccountName(newBillingAccount)
            .setBillingEnabled(false);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(
            workspace.getGoogleProject(), newBillingAccount))
        .thenReturn(projectBillingInfo);

    assertThrows(
        FailedPreconditionException.class,
        () -> workspaceService.updateWorkspaceBillingAccount(workspace, newBillingAccount));
  }

  @Test
  public void userWithoutCtTierAccessCTWorkspace() {
    DbWorkspace dbWorkspace =
        buildDbWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "Controlled Tier Workspace",
            DEFAULT_WORKSPACE_NAMESPACE,
            WorkspaceActiveStatus.ACTIVE);
    DbCdrVersion dbCdrVersion = createControlledTierCdrVersion(1);
    accessTierDao.save(dbCdrVersion.getAccessTier());
    cdrVersionDao.save(dbCdrVersion);
    dbWorkspace.setCdrVersion(dbCdrVersion);
    workspaceDao.save(dbWorkspace);
    assertThrows(
        ForbiddenException.class,
        () ->
            workspaceService.getWorkspace(
                DEFAULT_WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName()));
  }

  @Test
  public void userWithoutRegisterTierAccessRTWorkspace() {
    DbWorkspace dbWorkspace = dbWorkspaces.get(0);
    DbCdrVersion dbCdrVersion = createDefaultCdrVersion();
    accessTierDao.save(dbCdrVersion.getAccessTier());
    cdrVersionDao.save(dbCdrVersion);
    dbWorkspace.setCdrVersion(dbCdrVersion);
    when(accessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(Collections.singletonList(AccessTierService.CONTROLLED_TIER_SHORT_NAME));
    assertThrows(
        ForbiddenException.class,
        () ->
            workspaceService.getWorkspace(
                DEFAULT_WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName()));
  }

  @Test
  public void userWithCtTierAccessCTWorkspace() {
    DbWorkspace dbWorkspace =
        buildDbWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "Controlled Tier Workspace",
            DEFAULT_WORKSPACE_NAMESPACE,
            WorkspaceActiveStatus.ACTIVE);
    DbCdrVersion dbCdrVersion = createControlledTierCdrVersion(1);
    accessTierDao.save(dbCdrVersion.getAccessTier());
    cdrVersionDao.save(dbCdrVersion);
    dbWorkspace.setCdrVersion(dbCdrVersion);

    addMockedWorkspace(dbWorkspace);

    when(accessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(Collections.singletonList(AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    workspaceService.getWorkspace(DEFAULT_WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName());
  }
}
