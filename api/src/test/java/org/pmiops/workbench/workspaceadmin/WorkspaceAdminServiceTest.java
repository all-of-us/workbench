package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.ListRuntimeResponse;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceAdminServiceTest {

  private static final long DB_WORKSPACE_ID = 2222L;
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String WORKSPACE_NAME = "Gone with the Wind";

  @MockBean private CloudMonitoringService mockCloudMonitoringService;
  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private NotebooksService mockNotebooksService;
  @MockBean private WorkspaceDao mockWorkspaceDao;

  @Autowired private WorkspaceAdminService workspaceAdminService;

  @TestConfiguration
  @Import({
    CohortMapperImpl.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceMapperImpl.class,
    LeonardoMapperImpl.class
  })
  @MockBean({
    ActionAuditQueryService.class,
    AdminAuditor.class,
    CloudStorageService.class,
    CohortDao.class,
    CohortReviewMapper.class,
    CommonMappers.class,
    ConceptSetDao.class,
    ConceptSetMapper.class,
    DataSetDao.class,
    DataSetMapper.class,
    FirecloudMapper.class,
    LeonardoNotebooksClient.class,
    UserDao.class,
    UserMapper.class,
    UserService.class,
    WorkspaceService.class
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig getConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }
  }

  @Before
  public void setUp() {
    final TestMockFactory testMockFactory = new TestMockFactory();

    when(mockFirecloudService.getWorkspaceAsService(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(
                    new FirecloudWorkspace().bucketName("bucket").namespace(WORKSPACE_NAMESPACE)));

    final Workspace workspace =
        testMockFactory.createWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME);
    final DbWorkspace dbWorkspace =
        TestMockFactory.createDbWorkspaceStub(workspace, DB_WORKSPACE_ID);
    doReturn(Optional.of(dbWorkspace))
        .when(mockWorkspaceDao)
        .findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(WORKSPACE_NAMESPACE);
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
  public void testGetAdminWorkspaceCloudStorageCounts() {
    AdminWorkspaceCloudStorageCounts resp =
        workspaceAdminService.getAdminWorkspaceCloudStorageCounts("foo", "bar");
    assertThat(resp)
        .isEqualTo(
            new AdminWorkspaceCloudStorageCounts()
                .storageBucketPath("gs://bucket")
                .nonNotebookFileCount(0)
                .notebookFileCount(0)
                .storageBytesUsed(0L)
                .storageBucketPath("gs://bucket"));
    verify(mockNotebooksService, atLeastOnce()).getNotebooksAsService(any());

    // Regression check: the admin service should never call the end-user variants of these methods.
    verify(mockNotebooksService, never()).getNotebooks(any(), any());
    verify(mockFirecloudService, never()).getWorkspace(any(), any());
  }

  @Test
  public void testGetWorkspaceAdminView() {

    WorkspaceAdminView workspaceDetailsResponse =
        workspaceAdminService.getWorkspaceAdminView(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getNamespace())
        .isEqualTo(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getName()).isEqualTo(WORKSPACE_NAME);

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

    List<ListRuntimeResponse> runtimes = resources.getRuntimes();
    assertThat(runtimes).isEmpty();
  }
}
