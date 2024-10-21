package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.Date;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import jakarta.mail.MessagingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ClusterStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AdminLockingRequest;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ListRuntimeDeleteRequest;
import org.pmiops.workbench.model.PublishWorkspaceRequest;
import org.pmiops.workbench.model.TimeSeriesPoint;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.notebooks.NotebookUtils;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class WorkspaceAdminServiceTest {

  private static final String GOOGLE_PROJECT_ID = DEFAULT_GOOGLE_PROJECT;
  private static final String GOOGLE_PROJECT_ID_2 = "aou-gcp-id-2";
  private static final String WORKSPACE_NAMESPACE = "aou-rw-12345";
  private static final String WORKSPACE_DISPLAY_NAME = "Work It !";
  private static final String WORKSPACE_TERRA_NAME = "workit";
  private static final String CREATED_DATE = Date.fromYearMonthDay(1988, 12, 26).toString();
  private static final String RUNTIME_NAME = "all-of-us-runtime";
  private static final String RUNTIME_NAME_2 = "all-of-us-runtime-2";
  private static final String EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT = "all-of-us-different-project";
  private static WorkbenchConfig providedWorkbenchConfig;

  private DbWorkspace dbWorkspace;
  private GetRuntimeResponse testLeoRuntime;
  private GetRuntimeResponse testLeoRuntimeDifferentProject;
  private ListRuntimeResponse testLeoListRuntimeResponse;
  private ListRuntimeResponse testLeoListRuntimeResponse2;

  @MockBean private AdminAuditor mockAdminAuditor;
  @MockBean private CloudMonitoringService mockCloudMonitoringService;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockBean private FeaturedWorkspaceMapper mockFeaturedWorkspaceMapper;
  @MockBean private FireCloudService mockFirecloudService;
  @MockBean private LeonardoApiClient mockLeonardoNotebooksClient;
  @MockBean private LeonardoRuntimeAuditor mockLeonardoRuntimeAuditor;
  @MockBean private MailService mailService;
  @MockBean private NotebooksService mockNotebooksService;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceAdminService workspaceAdminService;

  private DbCdrVersion cdrVersion;
  private static DbUser currentUser;

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    CohortMapperImpl.class,
    FakeClockConfiguration.class,
    LeonardoMapperImpl.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceMapperImpl.class,
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
    InitialCreditsExpirationService.class,
    LeonardoApiClient.class,
    UserMapper.class,
    UserService.class,
    WorkspaceAuthService.class,
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
    when(mockFirecloudService.getWorkspaceAsService(any(), any()))
        .thenReturn(
            new RawlsWorkspaceResponse()
                .workspace(
                    new RawlsWorkspaceDetails()
                        .bucketName("bucket")
                        .namespace(WORKSPACE_NAMESPACE)));

    final Workspace workspace =
        TestMockFactory.createWorkspace(
            WORKSPACE_NAMESPACE, WORKSPACE_DISPLAY_NAME, WORKSPACE_TERRA_NAME);
    dbWorkspace = workspaceDao.save(TestMockFactory.createDbWorkspaceStub(workspace, 1L));

    when(mockFirecloudService.getGroup(anyString()))
        .thenReturn(new FirecloudManagedGroupWithMembers().groupEmail("test@firecloud.org"));

    testLeoRuntime =
        new GetRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(ClusterStatus.DELETING)
            .auditInfo(new AuditInfo().createdDate(CREATED_DATE));
    testLeoListRuntimeResponse =
        new ListRuntimeResponse()
            .runtimeName(RUNTIME_NAME)
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(ClusterStatus.RUNNING);

    testLeoListRuntimeResponse2 =
        new ListRuntimeResponse()
            .runtimeName(RUNTIME_NAME_2)
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID))
            .status(ClusterStatus.RUNNING);

    testLeoRuntimeDifferentProject =
        new GetRuntimeResponse()
            .runtimeName(EXTRA_RUNTIME_NAME_DIFFERENT_PROJECT)
            .cloudContext(
                new CloudContext()
                    .cloudProvider(CloudProvider.GCP)
                    .cloudResource(GOOGLE_PROJECT_ID_2))
            .status(ClusterStatus.RUNNING)
            .auditInfo(new AuditInfo().createdDate(CREATED_DATE));
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
    verify(mockNotebooksService, atLeastOnce())
        .getNotebooksAsService(any(), anyString(), anyString());

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

  @Test
  public void testGetWorkspaceAdminView_featuredCategory() {

    WorkspaceAdminView workspaceDetailsResponse =
        workspaceAdminService.getWorkspaceAdminView(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getNamespace())
        .isEqualTo(WORKSPACE_NAMESPACE);
    assertThat(workspaceDetailsResponse.getWorkspace().getName()).isEqualTo(WORKSPACE_DISPLAY_NAME);
    assertThat(workspaceDetailsResponse.getWorkspace().getDisplayName())
        .isEqualTo(WORKSPACE_DISPLAY_NAME);

    assertThat(workspaceDetailsResponse.getWorkspace().getFeaturedCategory())
        .isEqualTo(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
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
  public void testListFilesJustAppFiles() {
    final List<Blob> blobs =
        ImmutableList.of(
            mockBlob("bucket", NotebookUtils.withNotebookPath("test.ipynb"), 1000L),
            mockBlob("bucket", NotebookUtils.withNotebookPath("test2.ipynb"), 2000L),
            mockBlob("bucket", NotebookUtils.withNotebookPath("scratch.txt"), 123L),
            mockBlob(
                "bucket", NotebookUtils.withNotebookPath("hidden/sneaky.ipynb"), 1000L * 1000L));
    when(mockCloudStorageClient.getBlobPage("bucket")).thenReturn(blobs);

    final List<FileDetail> expectedNotebookFiles =
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
                .name("sneaky.ipynb")
                .path("gs://bucket/notebooks/hidden/sneaky.ipynb")
                .sizeInBytes(1000L * 1000L)
                .lastModifiedTime(dummyTime));

    when(mockNotebooksService.getNotebooksAsService(anyString(), anyString(), anyString()))
        .thenReturn(expectedNotebookFiles);

    final List<FileDetail> files = workspaceAdminService.listFiles(WORKSPACE_NAMESPACE, true);
    assertThat(files).containsExactlyElementsIn(expectedNotebookFiles);
  }

  @Test
  public void testListFilesAllFilesInBucket() {
    final List<Blob> blobs =
        ImmutableList.of(
            mockBlob("bucket", NotebookUtils.withNotebookPath("test.ipynb"), 1000L),
            mockBlob("bucket", NotebookUtils.withNotebookPath("test2.ipynb"), 2000L),
            mockBlob("bucket", NotebookUtils.withNotebookPath("scratch.txt"), 123L),
            mockBlob(
                "bucket", NotebookUtils.withNotebookPath("hidden/sneaky.ipynb"), 1000L * 1000L));
    when(mockCloudStorageClient.getBlobPage("bucket")).thenReturn(blobs);

    final List<FileDetail> expectedAllfiles =
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
                .name("sneaky.ipynb")
                .path("gs://bucket/notebooks/hidden/sneaky.ipynb")
                .sizeInBytes(1000L * 1000L)
                .lastModifiedTime(dummyTime),
            new FileDetail()
                .name("scratch.txt")
                .path("gs://bucket/notebooks/hidden/scratch.txt")
                .sizeInBytes(1000L * 1000L)
                .lastModifiedTime(dummyTime));

    when(mockCloudStorageClient.blobToFileDetail(any(), anyString(), anySet()))
        .thenReturn(
            expectedAllfiles.get(0),
            expectedAllfiles.get(1),
            expectedAllfiles.get(2),
            expectedAllfiles.get(3));

    final List<FileDetail> files = workspaceAdminService.listFiles(WORKSPACE_NAMESPACE, false);
    assertThat(files).containsExactlyElementsIn(expectedAllfiles);
  }

  @Test
  public void testDeleteRuntimesInProject() {
    List<ListRuntimeResponse> listRuntimeResponseList = List.of(testLeoListRuntimeResponse);
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
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_DeleteSome() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        List.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete = List.of(testLeoRuntime.getRuntimeName());
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
  public void testDeleteRuntimesInProject_DeleteDoesNotAffectOtherProjects() {
    List<ListRuntimeResponse> listRuntimeResponseList =
        List.of(testLeoListRuntimeResponse, testLeoListRuntimeResponse2);
    List<String> runtimesToDelete = List.of(testLeoRuntimeDifferentProject.getRuntimeName());
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
  public void testDeleteRuntimesInProject_NoRuntimes() {
    List<ListRuntimeResponse> listRuntimeResponseList = List.of(testLeoListRuntimeResponse);
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
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testDeleteRuntimesInProject_NullRuntimesList() {
    List<ListRuntimeResponse> listRuntimeResponseList = List.of(testLeoListRuntimeResponse);
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
                .map(ListRuntimeResponse::getRuntimeName)
                .collect(Collectors.toList()));
  }

  @Test
  public void testSetAdminLockedStateCallsAuditor() {
    AdminLockingRequest adminLockingRequest = new AdminLockingRequest();
    adminLockingRequest.setRequestReason("To test auditor");
    adminLockingRequest.setRequestDateInMillis(12345677l);
    workspaceAdminService.setAdminLockedState(WORKSPACE_NAMESPACE, adminLockingRequest);
    verify(mockAdminAuditor)
        .fireLockWorkspaceAction(dbWorkspace.getWorkspaceId(), adminLockingRequest);
  }

  @Test
  public void testSetAdminUnlockedStateCallsAuditor() {
    workspaceAdminService.setAdminUnlockedState(WORKSPACE_NAMESPACE);
    verify(mockAdminAuditor).fireUnlockWorkspaceAction(dbWorkspace.getWorkspaceId());
  }

  @Test
  public void testPublishWorkspaceViaDB() throws MessagingException {
    testPublish(FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY, DbFeaturedCategory.PHENOTYPE_LIBRARY);
    verify(mailService, never()).sendPublishCommunityWorkspaceEmails(any(), any());
  }

  // differs only in that it sends emails
  @Test
  public void testPublishWorkspaceViaDB_Community() throws MessagingException {
    testPublish(FeaturedWorkspaceCategory.COMMUNITY, DbFeaturedCategory.COMMUNITY);
    verify(mailService).sendPublishCommunityWorkspaceEmails(any(), any());
  }

  private void testPublish(FeaturedWorkspaceCategory category, DbFeaturedCategory dbCategory) {
    // Arrange
    setupPublishWorkspaceMocks();

    DbWorkspace mockDbWorkspace = workspaceDao.save(stubWorkspace("ns", "n"));

    DbFeaturedWorkspace mockFeaturedWorkspace =
        new DbFeaturedWorkspace().setWorkspace(mockDbWorkspace).setCategory(dbCategory);

    when(mockFeaturedWorkspaceDao.save(any())).thenReturn(mockFeaturedWorkspace);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedWorkspace(
            any(PublishWorkspaceRequest.class), any(DbWorkspace.class)))
        .thenReturn(mockFeaturedWorkspace);

    // Act
    PublishWorkspaceRequest publishWorkspaceRequest =
        new PublishWorkspaceRequest().category(category);
    workspaceAdminService.publishWorkspaceViaDB(
        mockDbWorkspace.getWorkspaceNamespace(), publishWorkspaceRequest);

    // Assert
    verify(mockFeaturedWorkspaceDao).save(any());
    verify(mockAdminAuditor)
        .firePublishWorkspaceAction(mockDbWorkspace.getWorkspaceId(), category.toString(), null);

    // verify that the ACL update was performed as the RWB system, not as the admin user

    verify(mockFirecloudService)
        .updateWorkspaceAclForPublishing(
            mockDbWorkspace.getWorkspaceNamespace(), mockDbWorkspace.getFirecloudName(), true);
    verify(mockFirecloudService, never()).updateWorkspaceACL(anyString(), anyString(), any());
  }

  @Test
  public void testPublishWorkspaceViaDB_updateWithDifferentCategory() throws MessagingException {

    // Arrange
    setupPublishWorkspaceMocks();

    DbWorkspace mockDbWorkspace = workspaceDao.save(stubWorkspace("ns", "n"));

    DbFeaturedWorkspace existingDbFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(mockDbWorkspace)
            .setCategory(DbFeaturedCategory.DEMO_PROJECTS);

    DbFeaturedWorkspace dbFeaturedWorkspaceToSave =
        new DbFeaturedWorkspace()
            .setWorkspace(mockDbWorkspace)
            .setCategory(DbFeaturedCategory.COMMUNITY);

    when(mockFeaturedWorkspaceDao.findByWorkspace(mockDbWorkspace))
        .thenReturn(Optional.of(existingDbFeaturedWorkspace));

    PublishWorkspaceRequest request =
        new PublishWorkspaceRequest().category(FeaturedWorkspaceCategory.COMMUNITY);
    when(mockFeaturedWorkspaceMapper.toDbFeaturedWorkspace(existingDbFeaturedWorkspace, request))
        .thenReturn(dbFeaturedWorkspaceToSave);

    // Act
    workspaceAdminService.publishWorkspaceViaDB(mockDbWorkspace.getWorkspaceNamespace(), request);

    // Assert
    verify(mockFeaturedWorkspaceDao).save(any());
    verify(mockAdminAuditor)
        .firePublishWorkspaceAction(
            mockDbWorkspace.getWorkspaceId(),
            dbFeaturedWorkspaceToSave.getCategory().toString(),
            existingDbFeaturedWorkspace.getCategory().toString());
    // does not send emails because it's an update, not a new publish
    verify(mailService, never()).sendPublishCommunityWorkspaceEmails(any(), any());

    // We should not update the ACL as we are just updating the category and the workspace is
    // already published
    verify(mockFirecloudService, never())
        .updateWorkspaceAclForPublishing(
            mockDbWorkspace.getWorkspaceNamespace(), mockDbWorkspace.getFirecloudName(), true);
    verify(mockFirecloudService, never()).updateWorkspaceACL(anyString(), anyString(), any());
  }

  @Test
  public void testPublishWorkspaceViaDB_updateWithSameCategory() throws MessagingException {

    // Arrange
    setupPublishWorkspaceMocks();

    DbWorkspace workspace = workspaceDao.save(stubWorkspace("ns", "n"));

    DbFeaturedWorkspace mockFeaturedWorkspace =
        new DbFeaturedWorkspace().setWorkspace(workspace).setCategory(DbFeaturedCategory.COMMUNITY);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedWorkspace(
            any(DbFeaturedWorkspace.class), any(PublishWorkspaceRequest.class)))
        .thenReturn(mockFeaturedWorkspace);
    when(mockFeaturedWorkspaceDao.findByWorkspace(workspace))
        .thenReturn(Optional.of(mockFeaturedWorkspace));
    when(mockFeaturedWorkspaceDao.save(any())).thenReturn(mockFeaturedWorkspace);

    // Act
    PublishWorkspaceRequest request =
        new PublishWorkspaceRequest().category(FeaturedWorkspaceCategory.COMMUNITY);
    workspaceAdminService.publishWorkspaceViaDB(workspace.getWorkspaceNamespace(), request);

    // Assert
    // Since the category is the same, we should not save the workspace again or send emails
    verify(mockFeaturedWorkspaceDao, never()).save(any());
    verify(mockAdminAuditor, never())
        .firePublishWorkspaceAction(
            workspace.getWorkspaceId(), request.getCategory().toString(), "");
    // does not send emails because it's an update, not a new publish
    verify(mailService, never()).sendPublishCommunityWorkspaceEmails(any(), any());
  }

  @Test
  public void testUnpublishWorkspaceViaDb() throws MessagingException {

    // Arrange
    setupPublishWorkspaceMocks();

    DbWorkspace mockDbWorkspace = workspaceDao.save(stubWorkspace("ns", "n"));
    DbFeaturedWorkspace mockFeaturedworkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(mockDbWorkspace)
            .setCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);
    when(mockFeaturedWorkspaceDao.findByWorkspace(mockDbWorkspace))
        .thenReturn(Optional.of(mockFeaturedworkspace));

    // Act
    workspaceAdminService.unpublishWorkspaceViaDB(mockDbWorkspace.getWorkspaceNamespace());

    // Assert
    verify(mockFeaturedWorkspaceDao).delete(any());
    verify(mockAdminAuditor)
        .fireUnpublishWorkspaceAction(mockDbWorkspace.getWorkspaceId(), "TUTORIAL_WORKSPACES");
    verify(mailService).sendAdminUnpublishWorkspaceEmails(any(), any());

    // verify that the ACL update was performed as the RWB system, not as the admin user

    verify(mockFirecloudService)
        .updateWorkspaceAclForPublishing(
            mockDbWorkspace.getWorkspaceNamespace(), mockDbWorkspace.getFirecloudName(), false);
    verify(mockFirecloudService, never()).updateWorkspaceACL(anyString(), anyString(), any());
  }

  private DbWorkspace stubWorkspace(String namespace, String name) {
    return new DbWorkspace()
        .setCdrVersion(cdrVersion)
        .setWorkspaceNamespace(namespace)
        .setName(name)
        .setFirecloudName("fc-" + name);
  }

  private DbWorkspace mustGetDbWorkspace(DbWorkspace w) {
    return workspaceDao.findDbWorkspaceByWorkspaceId(w.getWorkspaceId());
  }

  private void setupPublishWorkspaceMocks() {
    String rtAuthDomainGroupEmail = "rt@broad.org";
    when(mockWorkspaceService.getPublishedWorkspacesGroupEmail())
        .thenReturn(rtAuthDomainGroupEmail);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedCategory(
            FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES))
        .thenReturn(DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedCategory(FeaturedWorkspaceCategory.DEMO_PROJECTS))
        .thenReturn(DbFeaturedCategory.DEMO_PROJECTS);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedCategory(
            FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY))
        .thenReturn(DbFeaturedCategory.PHENOTYPE_LIBRARY);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedCategory(FeaturedWorkspaceCategory.COMMUNITY))
        .thenReturn(DbFeaturedCategory.COMMUNITY);

    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(
            DbFeaturedCategory.TUTORIAL_WORKSPACES))
        .thenReturn(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(DbFeaturedCategory.DEMO_PROJECTS))
        .thenReturn(FeaturedWorkspaceCategory.DEMO_PROJECTS);

    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(
            DbFeaturedCategory.PHENOTYPE_LIBRARY))
        .thenReturn(FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY);

    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(DbFeaturedCategory.COMMUNITY))
        .thenReturn(FeaturedWorkspaceCategory.COMMUNITY);
  }
}
