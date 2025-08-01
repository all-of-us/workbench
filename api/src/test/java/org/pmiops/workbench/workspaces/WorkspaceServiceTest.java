package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTierCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
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
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.profile.ProfileMapper;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
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
    CloudStorageClientImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    ObjectNameLengthServiceImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class
  })
  @MockBean({
    AccessTierService.class,
    BillingProjectAuditor.class,
    BucketAuditQueryService.class,
    CohortCloningService.class,
    CohortService.class,
    ConceptSetService.class,
    DataSetService.class,
    EgressRemediationService.class,
    FeaturedWorkspaceMapper.class,
    IamService.class,
    InitialCreditsService.class,
    ProfileMapper.class,
    UserDao.class,
    UserMapper.class,
    UserService.class
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

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    Stopwatch stopwatch() {
      return Stopwatch.createUnstarted();
    }

    @Bean
    @Qualifier("objectLengthsEgressService")
    EgressRemediationService objectLengthsEgressService() {
      return mock(EgressRemediationService.class);
    }
  }

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

  @MockBean private AccessTierService mockAccessTierService;
  @MockBean private BillingProjectAuditor mockBillingProjectAuditor;
  @MockBean private Clock mockClock;
  @MockBean private CloudBillingClient mockCloudBillingClient;
  @MockBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private MailService mockMailService;
  @MockBean private WorkspaceAuthService mockWorkspaceAuthService;
  @MockBean private Provider<Stopwatch> mockStopwatchProvider;

  private static DbUser currentUser;

  private final List<RawlsWorkspaceListResponse> firecloudWorkspaceResponses = new ArrayList<>();
  private final List<DbWorkspace> dbWorkspaces = new ArrayList<>();
  private static final Instant NOW = Instant.parse("1985-11-05T22:04:00.00Z");
  private static final long USER_ID = 1L;
  private static final String DEFAULT_USERNAME = "mock@mock.com";
  private static final String DEFAULT_WORKSPACE_NAMESPACE = "namespace";

  private final AtomicLong workspaceIdIncrementer = new AtomicLong(1);

  private static WorkbenchConfig workbenchConfig;

  @BeforeEach
  public void setUp() {
    doReturn(NOW).when(mockClock).instant();

    // Mock the Stopwatch provider
    Stopwatch mockStopwatch = Stopwatch.createUnstarted();
    doReturn(mockStopwatch).when(mockStopwatchProvider).get();

    firecloudWorkspaceResponses.clear();
    dbWorkspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(firecloudWorkspaceResponses).when(mockFireCloudService).listWorkspaces();

    currentUser = new DbUser();
    currentUser.setUsername(DEFAULT_USERNAME);
    currentUser.setUserId(USER_ID);
    currentUser.setDisabled(false);

    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.accountId = "initial-credits";
  }

  // Test data record for workspace scenarios
  record WorkspaceTestData(
      String namespace, String uuid, String firecloudName, WorkspaceActiveStatus status) {
    WorkspaceTestData(String namespace, String uuid, WorkspaceActiveStatus status) {
      this(namespace, uuid, "firecloud-" + namespace, status);
    }
  }

  // Helper method to setup Terra workspaces mock
  private void setupTerraWorkspaces(List<WorkspaceTestData> terraWorkspaces) {
    List<RawlsWorkspaceListResponse> terraResponses =
        terraWorkspaces.stream()
            .map(ws -> createMockTerraWorkspace(ws.uuid(), ws.namespace()))
            .collect(Collectors.toList());
    doReturn(terraResponses).when(mockFireCloudService).listWorkspacesAsService();
  }

  // Helper method to setup database workspaces
  private void setupDatabaseWorkspaces(List<WorkspaceTestData> dbWorkspaces) {
    workspaceDao.deleteAll();
    dbWorkspaces.forEach(
        ws -> {
          DbWorkspace workspace =
              createWorkspace()
                  .setWorkspaceNamespace(ws.namespace())
                  .setFirecloudUuid(ws.uuid())
                  .setFirecloudName(ws.firecloudName())
                  .setWorkspaceActiveStatusEnum(ws.status());
          workspaceDao.save(workspace);
        });
  }

  // Helper method to create database workspace with specific details
  private DbWorkspace createDbWorkspaceWithDetails(
      String namespace, String uuid, String firecloudName, WorkspaceActiveStatus status) {
    return createWorkspace()
        .setWorkspaceNamespace(namespace)
        .setFirecloudUuid(uuid)
        .setFirecloudName(firecloudName)
        .setWorkspaceActiveStatusEnum(status);
  }

  // Helper method for common assertion pattern
  private void assertOrphanedNamespaces(List<String> expected) {
    List<String> orphanedNamespaces = workspaceService.getOrphanedWorkspaceNamespacesAsService();
    if (expected.isEmpty()) {
      assertThat(orphanedNamespaces).isEmpty();
    } else {
      assertThat(orphanedNamespaces).containsExactlyElementsIn(expected);
    }
  }

  private RawlsWorkspaceDetails createMockWorkspaceDetails(
      String workspaceTerraUuid, String workspaceTerraName, String workspaceNamespace) {
    return new RawlsWorkspaceDetails()
        .workspaceId(workspaceTerraUuid)
        .name(workspaceTerraName)
        .namespace(workspaceNamespace);
  }

  private RawlsWorkspaceResponse mockRawlsWorkspaceResponse(
      String workspaceTerraUuid,
      String workspaceTerraName,
      String workspaceNamespace,
      RawlsWorkspaceAccessLevel accessLevel) {
    RawlsWorkspaceDetails mockWorkspace =
        createMockWorkspaceDetails(workspaceTerraUuid, workspaceTerraName, workspaceNamespace);

    RawlsWorkspaceResponse mockWorkspaceResponse = new RawlsWorkspaceResponse();
    mockWorkspaceResponse.workspace(mockWorkspace);
    mockWorkspaceResponse.accessLevel(accessLevel);

    doReturn(mockWorkspaceResponse)
        .when(mockFireCloudService)
        .getWorkspace(workspaceNamespace, workspaceTerraName);
    return mockWorkspaceResponse;
  }

  private RawlsWorkspaceListResponse mockRawlsWorkspaceListResponse(
      String workspaceTerraUuid,
      String workspaceTerraName,
      String workspaceNamespace,
      RawlsWorkspaceAccessLevel accessLevel) {
    RawlsWorkspaceDetails mockWorkspace =
        createMockWorkspaceDetails(workspaceTerraUuid, workspaceTerraName, workspaceNamespace);

    RawlsWorkspaceListResponse mockWorkspaceListResponse = new RawlsWorkspaceListResponse();
    mockWorkspaceListResponse.setAccessLevel(accessLevel);
    mockWorkspaceListResponse.setWorkspace(mockWorkspace);
    return mockWorkspaceListResponse;
  }

  private DbWorkspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    DbWorkspace dbWorkspace = new DbWorkspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    dbWorkspace.setLastModifiedTime(nowTimestamp);
    dbWorkspace.setCreationTime(nowTimestamp);
    dbWorkspace.setName(name);
    dbWorkspace.setWorkspaceId(dbId);
    dbWorkspace.setWorkspaceNamespace(namespace);
    dbWorkspace.setWorkspaceActiveStatusEnum(activeStatus);
    dbWorkspace.setFirecloudName(name);
    dbWorkspace.setFirecloudUuid(Long.toString(dbId));
    return dbWorkspace;
  }

  private DbWorkspace addMockedWorkspace(
      long workspaceId,
      String workspaceTerraName,
      String workspaceNamespace,
      RawlsWorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    // in reality, these will NOT match
    String workspaceTerraUuid = Long.toString(workspaceId);

    RawlsWorkspaceResponse mockWorkspaceResponse =
        mockRawlsWorkspaceResponse(
            workspaceTerraUuid, workspaceTerraName, workspaceNamespace, accessLevel);

    RawlsWorkspaceListResponse mockWorkspaceListResponse =
        mockRawlsWorkspaceListResponse(
            workspaceTerraUuid, workspaceTerraName, workspaceNamespace, accessLevel);

    firecloudWorkspaceResponses.add(mockWorkspaceListResponse);

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

  private void addMockedWorkspace(DbWorkspace dbWorkspace) {

    mockRawlsWorkspaceResponse(
        dbWorkspace.getFirecloudUuid(),
        dbWorkspace.getName(),
        dbWorkspace.getWorkspaceNamespace(),
        RawlsWorkspaceAccessLevel.OWNER);

    workspaceDao.save(dbWorkspace);
    dbWorkspaces.add(dbWorkspace);
  }

  private void addMockedPublishedWorkspace(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      DbFeaturedCategory featuredCategory,
      WorkspaceActiveStatus activeStatus,
      RawlsWorkspaceAccessLevel accessLevel) {
    DbWorkspace dbWorkspace =
        workspaceDao
            .save(buildDbWorkspace(workspaceId, workspaceName, workspaceNamespace, activeStatus))
            .setFeaturedCategory(featuredCategory);

    dbWorkspaces.add(dbWorkspace);

    RawlsWorkspaceListResponse mockWorkspaceListResponse =
        mockRawlsWorkspaceListResponse(
            Long.toString(dbWorkspace.getWorkspaceId()),
            workspaceName,
            workspaceNamespace,
            accessLevel);
    firecloudWorkspaceResponses.add(mockWorkspaceListResponse);
    when(mockFireCloudService.listWorkspaces()).thenReturn(firecloudWorkspaceResponses);
  }

  @Test
  public void listWorkspaces() {
    assertThat(workspaceService.listWorkspaces()).hasSize(5);
  }

  @Test
  public void listWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.listWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "deleted",
        DEFAULT_WORKSPACE_NAMESPACE,
        RawlsWorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.listWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void listWorkspaces_skipPublished() {
    int currentWorkspacesSize = workspaceService.listWorkspaces().size();
    addMockedPublishedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "published_reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        DbFeaturedCategory.TUTORIAL_WORKSPACES,
        WorkspaceActiveStatus.ACTIVE,
        RawlsWorkspaceAccessLevel.READER);

    assertThat(workspaceService.listWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void listWorkspaces_published_butOwner() {
    int currentWorkspacesSize = workspaceService.listWorkspaces().size();
    addMockedPublishedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "published_reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        DbFeaturedCategory.TUTORIAL_WORKSPACES,
        WorkspaceActiveStatus.ACTIVE,
        RawlsWorkspaceAccessLevel.OWNER);

    assertThat(workspaceService.listWorkspaces().size()).isEqualTo(currentWorkspacesSize + 1);
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
            RawlsWorkspaceAccessLevel.OWNER,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(ownedWorkspace);

    DbWorkspace sharedWorkspace =
        addMockedWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "shared",
            "shared_namespace",
            RawlsWorkspaceAccessLevel.NO_ACCESS,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(sharedWorkspace);
    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(
            "shared_namespace", "shared", WorkspaceAccessLevel.READER))
        .thenThrow(ForbiddenException.class);
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
    verify(mockFeaturedWorkspaceDao).deleteDbFeaturedWorkspaceByWorkspace(eq(ws));
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
    workspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());
    ProjectBillingInfo projectBillingInfo =
        new ProjectBillingInfo()
            .setProjectId(workspace.getGoogleProject())
            .setBillingAccountName(newBillingAccount)
            .setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(
            workspace.getGoogleProject(), newBillingAccount))
        .thenReturn(projectBillingInfo);

    assertThat(workspace.getBillingAccountName())
        .isEqualTo(workbenchConfig.billing.initialCreditsBillingAccountName());

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
            .setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName())
            .setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(
            workspace.getGoogleProject(),
            workbenchConfig.billing.initialCreditsBillingAccountName()))
        .thenReturn(projectBillingInfo);
    workspaceService.updateWorkspaceBillingAccount(
        workspace, workbenchConfig.billing.initialCreditsBillingAccountName());

    verify(mockFireCloudService)
        .updateBillingAccountAsService(
            workspace.getWorkspaceNamespace(),
            workbenchConfig.billing.initialCreditsBillingAccountName());
    verify(mockFireCloudService, never()).updateBillingAccount(anyString(), anyString());
    assertThat(workspace.getBillingAccountName())
        .isEqualTo(workbenchConfig.billing.initialCreditsBillingAccountName());
  }

  @Test
  public void updateBillingAccount_noChange() {
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
    workspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());

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
  public void testGetWorkspace_featuredCategory() {
    // Arrange
    DbWorkspace dbWorkspace =
        buildDbWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "Controlled Tier Workspace",
            DEFAULT_WORKSPACE_NAMESPACE,
            WorkspaceActiveStatus.ACTIVE);
    dbWorkspace.setFeaturedCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);

    DbCdrVersion dbCdrVersion = createControlledTierCdrVersion(1);
    accessTierDao.save(dbCdrVersion.getAccessTier());
    cdrVersionDao.save(dbCdrVersion);
    dbWorkspace.setCdrVersion(dbCdrVersion);
    addMockedWorkspace(dbWorkspace);
    dbWorkspace.setCreator(currentUser);

    when(mockAccessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(Collections.singletonList(AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    when(mockAccessTierService.getAccessTierShortNamesForUser(dbWorkspace.getCreator()))
        .thenReturn(Collections.singletonList(AccessTierService.CONTROLLED_TIER_SHORT_NAME));

    // Act
    WorkspaceResponse response =
        workspaceService.getWorkspace(DEFAULT_WORKSPACE_NAMESPACE, dbWorkspace.getFirecloudName());

    // Assert
    assertThat(response.getWorkspace().getFeaturedCategory())
        .isEqualTo(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }

  @Test
  public void testPublishAlreadyPublishedWorkspace() {
    // Arrange
    DbWorkspace dbWorkspace =
        buildDbWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "Controlled Tier Workspace",
            DEFAULT_WORKSPACE_NAMESPACE,
            WorkspaceActiveStatus.ACTIVE);

    DbFeaturedWorkspace mockDBFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(dbWorkspace)
            .setCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace))
        .thenReturn(Optional.of(mockDBFeaturedWorkspace));

    // Assert
    Exception alreadyPublishedException =
        assertThrows(
            BadRequestException.class,
            () -> workspaceService.publishCommunityWorkspace(dbWorkspace));

    assertEquals("Workspace is already published", alreadyPublishedException.getMessage());
  }

  @Test
  public void testPublishCommunityWorkspace() throws MessagingException {
    // Arrange
    DbWorkspace dbWorkspace =
        buildDbWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "Registered Tier Workspace",
            DEFAULT_WORKSPACE_NAMESPACE,
            WorkspaceActiveStatus.ACTIVE);

    DbFeaturedWorkspace mockDBFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(dbWorkspace)
            .setCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace)).thenReturn(Optional.empty());

    when(mockFeaturedWorkspaceDao.save(any())).thenReturn(mockDBFeaturedWorkspace);

    when(mockFireCloudService.getWorkspaceAsService(any(), any()))
        .thenReturn(
            new RawlsWorkspaceResponse()
                .workspace(
                    new RawlsWorkspaceDetails()
                        .bucketName("bucket")
                        .namespace(DEFAULT_WORKSPACE_NAMESPACE)));

    when(mockAccessTierService.getRegisteredTierOrThrow()).thenReturn(createRegisteredTier());

    // Act
    workspaceService.publishCommunityWorkspace(dbWorkspace);

    // Assert
    verify(mockFeaturedWorkspaceDao).save(any());
    verify(mockMailService).sendPublishCommunityWorkspaceEmails(any(), any());
  }

  @Test
  public void testGetFeaturedWorkspaces_all() {
    // start with none
    var before = workspaceService.getFeaturedWorkspaces();
    assertThat(before).isEmpty();

    dbWorkspaces.forEach(
        dbWorkspace ->
            dbWorkspace.setFeaturedCategory(
                DbFeaturedCategory.TUTORIAL_WORKSPACES)); // set all workspaces as featured

    var result = workspaceService.getFeaturedWorkspaces();
    assertThat(result).hasSize(dbWorkspaces.size());
  }

  @Test
  public void testGetFeaturedWorkspaces_one() {
    // arbitrary choice from dbWorkspaces
    var testWorkspace = dbWorkspaces.get(2);
    testWorkspace.setFeaturedCategory(DbFeaturedCategory.DEMO_PROJECTS);

    var result = workspaceService.getFeaturedWorkspaces();
    assertThat(result).hasSize(1);
    var resultWorkspace = result.get(0).getWorkspace();
    assertThat(resultWorkspace.getName()).isEqualTo(testWorkspace.getName());
    assertThat(resultWorkspace.getNamespace()).isEqualTo(testWorkspace.getWorkspaceNamespace());
    assertThat(resultWorkspace.getTerraName()).isEqualTo(testWorkspace.getFirecloudName());
    assertThat(resultWorkspace.getFeaturedCategory())
        .isEqualTo(FeaturedWorkspaceCategory.DEMO_PROJECTS);
  }

  // Test data for orphaned workspace scenarios
  static Stream<Arguments> orphanedWorkspaceAsServiceScenarios() {
    return Stream.of(
        Arguments.of(
            "Empty - no Terra workspaces, no DB workspaces",
            Collections.emptyList(), // Terra workspaces
            Collections.emptyList(), // DB workspaces
            Collections.emptyList() // Expected orphans
            ),
        Arguments.of(
            "No orphans - Terra and DB workspaces match",
            List.of(
                new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "namespace2", "uuid2", WorkspaceActiveStatus.ACTIVE)), // Terra workspaces
            List.of(
                new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "namespace2", "uuid2", WorkspaceActiveStatus.ACTIVE)), // DB workspaces
            Collections.emptyList() // Expected orphans
            ),
        Arguments.of(
            "Some orphans - some DB workspaces not in Terra",
            List.of(
                new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "namespace2", "uuid2", WorkspaceActiveStatus.ACTIVE)), // Terra workspaces
            List.of(
                new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData("namespace2", "uuid2", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "orphan-namespace1", "orphan-uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "orphan-namespace2",
                    "orphan-uuid2",
                    WorkspaceActiveStatus.ACTIVE)), // DB workspaces
            List.of("orphan-namespace1", "orphan-namespace2") // Expected orphans
            ),
        Arguments.of(
            "All orphans - no Terra workspaces, only DB workspaces",
            Collections.emptyList(), // Terra workspaces
            List.of(
                new WorkspaceTestData(
                    "orphan-namespace1", "orphan-uuid1", WorkspaceActiveStatus.ACTIVE),
                new WorkspaceTestData(
                    "orphan-namespace2",
                    "orphan-uuid2",
                    WorkspaceActiveStatus.ACTIVE)), // DB workspaces
            List.of("orphan-namespace1", "orphan-namespace2") // Expected orphans
            ));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("orphanedWorkspaceAsServiceScenarios")
  void testGetOrphanedWorkspaceNamespacesAsService_basicScenarios(
      String scenarioName,
      List<WorkspaceTestData> terraWorkspaces,
      List<WorkspaceTestData> dbWorkspaces,
      List<String> expectedOrphans) {
    // Arrange
    setupTerraWorkspaces(terraWorkspaces);
    setupDatabaseWorkspaces(dbWorkspaces);

    // Act & Assert
    assertOrphanedNamespaces(expectedOrphans);
  }

  @Test
  public void getOrphanedWorkspaceNamespacesAsService_excludesDeletedWorkspaces() {
    // Mock Terra returning some workspaces
    setupTerraWorkspaces(
        List.of(new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE)));

    // Add a combination of active and deleted workspaces to the database
    setupDatabaseWorkspaces(
        List.of(
            new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE),
            new WorkspaceTestData(
                "deleted-namespace", "deleted-uuid", WorkspaceActiveStatus.DELETED),
            new WorkspaceTestData(
                "orphan-namespace", "orphan-uuid", WorkspaceActiveStatus.ACTIVE)));

    // Should only return the active orphan, not the deleted workspace
    assertOrphanedNamespaces(List.of("orphan-namespace"));
  }

  @Test
  public void getOrphanedWorkspaceNamespacesAsService_duplicateNamespaces() {
    // Mock Terra returning some workspaces
    setupTerraWorkspaces(
        List.of(new WorkspaceTestData("namespace1", "uuid1", WorkspaceActiveStatus.ACTIVE)));

    // Add orphaned workspaces with the same namespace
    workspaceDao.deleteAll();
    DbWorkspace workspace1 =
        createDbWorkspaceWithDetails(
            "namespace1", "uuid1", "firecloud-name1", WorkspaceActiveStatus.ACTIVE);
    // Two orphan workspaces with same namespace
    DbWorkspace orphan1 =
        createDbWorkspaceWithDetails(
            "orphan-namespace",
            "orphan-uuid1",
            "orphan-firecloud-name1",
            WorkspaceActiveStatus.ACTIVE);
    DbWorkspace orphan2 =
        createDbWorkspaceWithDetails(
            "orphan-namespace",
            "orphan-uuid2",
            "orphan-firecloud-name2",
            WorkspaceActiveStatus.ACTIVE);

    workspaceDao.save(workspace1);
    workspaceDao.save(orphan1);
    workspaceDao.save(orphan2);

    // Should only return one instance of the duplicate namespace due to DISTINCT
    assertOrphanedNamespaces(List.of("orphan-namespace"));
  }

  private RawlsWorkspaceListResponse createMockTerraWorkspace(String uuid, String namespace) {
    return mockRawlsWorkspaceListResponse(
        uuid, "terra-name", namespace, RawlsWorkspaceAccessLevel.OWNER);
  }

  private DbWorkspace createWorkspace() {
    return buildDbWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "test-workspace-name",
        "test-workspace-namespace",
        WorkspaceActiveStatus.ACTIVE);
  }
}
