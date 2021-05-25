package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.cloud.Date;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
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
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
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

@ExtendWith(SpringExtension.class)
public class WorkspaceAdminServiceTest {

  private static final long DB_WORKSPACE_ID = 2222L;
  private static final String GOOGLE_PROJECT_ID = DEFAULT_GOOGLE_PROJECT;
  private static final String GOOGLE_PROJECT_ID_2 = "aou-gcp-id-2";
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String WORKSPACE_NAME = "Gone with the Wind";
  private static final String CREATED_DATE = Date.fromYearMonthDay(1988, 12, 26).toString();
  private static final String RUNTIME_NAME = "all-of-us-runtime";
  private static final String RUNTIME_NAME_2 = "all-of-us-runtime-2";
  private static final String EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT = "all-of-us-different-project";

  private LeonardoGetRuntimeResponse testLeoRuntime;
  private LeonardoGetRuntimeResponse testLeoRuntime2;
  private LeonardoGetRuntimeResponse testLeoRuntimeDifferentProject;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse;
  private LeonardoListRuntimeResponse testLeoListRuntimeResponse2;

  @MockBean private CloudMonitoringService mockCloudMonitoringService;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private NotebooksService mockNotebooksService;
  @MockBean private WorkspaceDao mockWorkspaceDao;
  @MockBean LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean LeonardoNotebooksClient mockLeonardoNotebooksClient;
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

  @BeforeEach
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

    // required to enable the use of default method blobToFileDetail()
    when(mockCloudStorageClient.blobToFileDetail(any(), anyString())).thenCallRealMethod();

    testLeoRuntime =
        new LeonardoGetRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.DELETING)
            .auditInfo(new LeonardoAuditInfo().createdDate(CREATED_DATE));
    testLeoListRuntimeResponse =
        new LeonardoListRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);
    testLeoRuntime2 =
        new LeonardoGetRuntimeResponse()
            .runtimeName(RUNTIME_NAME_2)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(CREATED_DATE));

    testLeoListRuntimeResponse2 =
        new LeonardoListRuntimeResponse()
            .runtimeName(RUNTIME_NAME_2)
            .googleProject(GOOGLE_PROJECT_ID)
            .status(LeonardoRuntimeStatus.RUNNING);

    testLeoRuntimeDifferentProject =
        new LeonardoGetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .googleProject(GOOGLE_PROJECT_ID_2)
            .status(LeonardoRuntimeStatus.RUNNING)
            .auditInfo(new LeonardoAuditInfo().createdDate(CREATED_DATE));
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

  private final long dummyTime = Instant.now().toEpochMilli();

  private Blob mockBlob(String bucket, String path, Long size) {
    Blob blob = mock(Blob.class);
    when(blob.getBlobId()).thenReturn(BlobId.of(bucket, path));
    when(blob.getBucket()).thenReturn(bucket);
    when(blob.getName()).thenReturn(path);
    when(blob.getSize()).thenReturn(size);
    when(blob.getUpdateTime()).thenReturn(dummyTime);
    return blob;
  }

  @Test
  public void testlistFiles() {
    final List<Blob> blobs =
        ImmutableList.of(
            mockBlob("bucket", "notebooks/test.ipynb", 1000L),
            mockBlob("bucket", "notebooks/test2.ipynb", 2000L),
            mockBlob("bucket", "notebooks/scratch.txt", 123L),
            mockBlob("bucket", "notebooks/hidden/sneaky.ipynb", 1000L * 1000L));
    when(mockCloudStorageClient.getBlobPage("bucket")).thenReturn(blobs);

    final List<FileDetail> expectedFiles =
        ImmutableList.of(
            new FileDetail()
                .name("test.ipynb")
                .path("gs://bucket/notebooks/test.ipynb")
                .sizeInBytes(1000L)
                .lastModifiedTime(dummyTime),
            new FileDetail()
                .name("test2.ipynb")
                .path("gs://bucket/notebooks/test2.ipynb")
                .sizeInBytes(2000L)
                .lastModifiedTime(dummyTime),
            new FileDetail()
                .name("scratch.txt")
                .path("gs://bucket/notebooks/scratch.txt")
                .sizeInBytes(123L)
                .lastModifiedTime(dummyTime),
            new FileDetail()
                .name("sneaky.ipynb")
                .path("gs://bucket/notebooks/hidden/sneaky.ipynb")
                .sizeInBytes(1000L * 1000L)
                .lastModifiedTime(dummyTime));

    final List<FileDetail> files = workspaceAdminService.listFiles(WORKSPACE_NAMESPACE);
    assertThat(files).containsExactlyElementsIn(expectedFiles);
  }

  @Test
  public void testDeleteRuntimesInProject() throws Exception {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(GOOGLE_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    workspaceAdminService.deleteRuntimesInWorkspace(
        WORKSPACE_NAMESPACE,
        new ListRuntimeDeleteRequest()
            .runtimesToDelete(ImmutableList.of(testLeoRuntime.getRuntimeName())));
    verify(mockLeonardoNotebooksClient)
        .deleteRuntimeAsService(GOOGLE_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            GOOGLE_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteSome() throws Exception {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete = ImmutableList.of(testLeoRuntime.getRuntimeName());
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(GOOGLE_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    workspaceAdminService.deleteRuntimesInWorkspace(
        WORKSPACE_NAMESPACE, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(mockLeonardoNotebooksClient, times(runtimesToDelete.size()))
        .deleteRuntimeAsService(GOOGLE_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(1))
        .fireDeleteRuntimesInProject(GOOGLE_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteDoesNotAffectOtherProjects() throws Exception {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete =
        ImmutableList.of(testLeoRuntimeDifferentProject.getRuntimeName());
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(GOOGLE_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    workspaceAdminService.deleteRuntimesInWorkspace(
        WORKSPACE_NAMESPACE, new ListRuntimeDeleteRequest().runtimesToDelete(runtimesToDelete));
    verify(mockLeonardoNotebooksClient, times(0))
        .deleteRuntimeAsService(GOOGLE_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, times(0))
        .fireDeleteRuntimesInProject(GOOGLE_PROJECT_ID, runtimesToDelete);
  }

  @Test
  public void testDeleteRuntimesInProject_NoRuntimes() throws Exception {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(GOOGLE_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    workspaceAdminService.deleteRuntimesInWorkspace(
        WORKSPACE_NAMESPACE, new ListRuntimeDeleteRequest().runtimesToDelete(ImmutableList.of()));
    verify(mockLeonardoNotebooksClient, never())
        .deleteRuntimeAsService(GOOGLE_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor, never())
        .fireDeleteRuntimesInProject(
            GOOGLE_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_NullRuntimesList() throws Exception {
    List<LeonardoListRuntimeResponse> listRuntimeResponseList =
        ImmutableList.of(testLeoListRuntimeResponse);
    when(mockLeonardoNotebooksClient.listRuntimesByProjectAsService(GOOGLE_PROJECT_ID))
        .thenReturn(listRuntimeResponseList);

    workspaceAdminService.deleteRuntimesInWorkspace(
        WORKSPACE_NAMESPACE, new ListRuntimeDeleteRequest().runtimesToDelete(null));
    verify(mockLeonardoNotebooksClient)
        .deleteRuntimeAsService(GOOGLE_PROJECT_ID, testLeoRuntime.getRuntimeName());
    verify(mockLeonardoRuntimeAuditor)
        .fireDeleteRuntimesInProject(
            GOOGLE_PROJECT_ID,
            listRuntimeResponseList.stream()
                .map(LeonardoListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }
}
