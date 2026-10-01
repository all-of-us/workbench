package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceServiceTest {

  @MockitoBean private ActionAuditQueryService actionAuditQueryService;
  @MockitoBean private BucketAuditQueryService bucketAuditQueryService;
  @MockitoBean private EgressRemediationService egressRemediationService;
  @MockitoBean private FeaturedWorkspaceMapper featuredWorkspaceMapper;
  @MockitoBean private IamService iamService;
  @MockitoBean private InitialCreditsService initialCreditsService;
  @MockitoBean private ProfileMapper profileMapper;
  @MockitoBean private ReportingQueryService reportingQueryService;
  @MockitoBean private UserDao userDao;
  @MockitoBean private UserMapper userMapper;
  @MockitoBean private UserService userService;

  @TestConfiguration
  @Import({
    CloudStorageClientImpl.class,
    CommonMappers.class,
    FirecloudMapperImpl.class,
    ObjectNameLengthServiceImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class
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

  @MockitoBean private AccessTierService mockAccessTierService;
  @MockitoBean private BillingProjectAuditor mockBillingProjectAuditor;
  @MockitoBean private Clock mockClock;
  @MockitoBean private CloudBillingClient mockCloudBillingClient;
  @MockitoBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockitoBean private FireCloudService mockFireCloudService;
  @MockitoBean private MailService mockMailService;
  @MockitoBean private WorkspaceAuthService mockWorkspaceAuthService;
  @MockitoBean private Provider<Stopwatch> mockStopwatchProvider;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

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

  @Test
  public void listWorkspaces() {
    assertThat(workspaceService.listWorkspaces()).hasSize(5);
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
}
