package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.Date;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.VwbAccessService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAuditInfo;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
public class WorkspaceAdminServiceTest {

  @MockitoBean private ActionAuditQueryService actionAuditQueryService;
  @MockitoBean private CohortDao cohortDao;
  @MockitoBean private CommonMappers commonMappers;
  @MockitoBean private ConceptSetDao conceptSetDao;
  @MockitoBean private DataSetDao dataSetDao;
  @MockitoBean private FirecloudMapper firecloudMapper;
  @MockitoBean private InitialCreditsService initialCreditsService;
  @MockitoBean private ReportingQueryService reportingQueryService;
  @MockitoBean private UserMapper userMapper;
  @MockitoBean private UserService userService;
  @MockitoBean private WorkspaceAuthService workspaceAuthService;
  private static final String GOOGLE_PROJECT_ID = DEFAULT_GOOGLE_PROJECT;
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String WORKSPACE_DISPLAY_NAME = "Work It !";
  private static final String WORKSPACE_TERRA_NAME = "workit";
  private static final String CREATED_DATE = Date.fromYearMonthDay(1988, 12, 26).toString();
  private static final String RUNTIME_NAME = "all-of-us-runtime";
  private static WorkbenchConfig providedWorkbenchConfig;

  private DbWorkspace dbWorkspace;
  private LeonardoGetRuntimeResponse testLeoRuntime;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse;

  @MockitoBean private AdminAuditor mockAdminAuditor;
  @MockitoBean private CloudMonitoringService mockCloudMonitoringService;
  @MockitoBean private CloudStorageClient mockCloudStorageClient;
  @MockitoBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockitoBean private FeaturedWorkspaceMapper mockFeaturedWorkspaceMapper;
  @MockitoBean private VwbAccessService mockVwbAccessService;
  @MockitoBean private LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockitoBean private MailService mailService;
  @MockitoBean private WorkspaceService mockWorkspaceService;

  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceAdminService workspaceAdminService;

  private DbCdrVersion cdrVersion;
  private static DbUser currentUser;

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    FakeClockConfiguration.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceMapperImpl.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }
  }

  @BeforeEach
  public void setUp() {
    currentUser = new DbUser();
    cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersionDao.save(cdrVersion);
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();

    final Workspace workspace =
        TestMockFactory.createWorkspace(
            WORKSPACE_NAMESPACE, WORKSPACE_DISPLAY_NAME, WORKSPACE_TERRA_NAME);
    dbWorkspace = workspaceDao.save(TestMockFactory.createDbWorkspaceStub(workspace, 1L));

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(LeonardoRuntimeStatus.DELETING)
            .auditInfo(new LeonardoAuditInfo().createdDate(CREATED_DATE));
    testLeoListRuntimeResponse =
        new LeonardoListRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .cloudContext(
                new LeonardoCloudContext()
                    .cloudProvider(LeonardoCloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(LeonardoRuntimeStatus.RUNNING);
  }

  @Test
  public void getCloudStorageTraffic_sortsPointsByTimestamp() {
    TimeSeries timeSeries =
        TimeSeries.newBuilder()
            .addPoints(
                Point.newBuilder()
                    .setInterval(TimeInterval.newBuilder().setEndTime(Timestamps.fromMillis(2000)))
                    .setValue(TypedValue.newBuilder().setDoubleValue(1234)))
            .addPoints(
                Point.newBuilder()
                    .setInterval(TimeInterval.newBuilder().setEndTime(Timestamps.fromMillis(1000)))
                    .setValue(TypedValue.newBuilder().setDoubleValue(1234)))
            .build();

    when(mockCloudMonitoringService.getCloudStorageReceivedBytes(anyString(), any(Duration.class)))
        .thenReturn(Collections.singletonList(timeSeries));

    final CloudStorageTraffic cloudStorageTraffic =
        workspaceAdminService.getCloudStorageTraffic(WORKSPACE_NAMESPACE);

    assertThat(
            cloudStorageTraffic.getReceivedBytes().stream()
                .map(TimeSeriesPoint::getTimestamp)
                .collect(Collectors.toList()))
        .containsExactly(1000L, 2000L);
  }

  @Test
  public void testGetWorkspaceAdminView() {

    WorkspaceAdminView workspaceDetailsResponse =
        workspaceAdminService.getWorkspaceAdminView(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getNamespace())
        .isEqualTo(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getName()).isEqualTo(WORKSPACE_DISPLAY_NAME);
    assertThat(workspaceDetailsResponse.getWorkspace().getDisplayName())
        .isEqualTo(WORKSPACE_DISPLAY_NAME);

    // TODO(jaycarlton): instrument mocks such that we can see actual counts here.
    //   The goal for today is just to move this test case here from WorkspaceAdminControllerTest,
    //   where all those counts were mocked anyway. I.e. we're not actually losing coverage, even
    //   though this looks trivial.
    AdminWorkspaceResources resources = workspaceDetailsResponse.getResources();
    AdminWorkspaceObjectsCounts objectsCounts = resources.getWorkspaceObjects();
    assertThat(objectsCounts.getCohortCount()).isEqualTo(0);
    assertThat(objectsCounts.getConceptSetCount()).isEqualTo(0);
    assertThat(objectsCounts.getDatasetCount()).isEqualTo(0);

    AdminWorkspaceCloudStorageCounts cloudStorageCounts = resources.getCloudStorage();
    assertThat(cloudStorageCounts.getStorageBucketPath()).isEqualTo("gs://bucket");
    assertThat(cloudStorageCounts.getNotebookFileCount()).isEqualTo(0);
    assertThat(cloudStorageCounts.getNonNotebookFileCount()).isEqualTo(0);
    assertThat(cloudStorageCounts.getStorageBytesUsed()).isEqualTo(0L);
  }
}
