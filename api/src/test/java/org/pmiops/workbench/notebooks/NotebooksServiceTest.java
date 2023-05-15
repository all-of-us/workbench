package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.MockNotebook;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class NotebooksServiceTest {
  private static final String BUCKET_NAME = "BUCKET_NAME";
  private static final String NOTEBOOK_NAME = "my first notebook.ipynb";
  private static final String NAMESPACE_NAME = "namespace_name";
  private static final String WORKSPACE_NAME = "workspace_name";
  private static final String PREVIOUS_NOTEBOOK = "previous notebook";

  private static DbUser dbUser;
  private static DbWorkspace dbWorkspace;
  private DbCdrVersion fromCDRVersion;
  private DbCdrVersion toCDRVersion;
  private DbAccessTier fromAccessTier;
  private DbAccessTier toAccessTier;

  @MockBean private LogsBasedMetricService mockLogsBasedMetricsService;

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private WorkspaceDao workspaceDao;
  @MockBean private UserRecentResourceService mockUserRecentResourceService;
  @MockBean private WorkspaceAuthService mockWorkspaceAuthService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private UserDao userDao;
  @Autowired private FirecloudMapper firecloudMapper;

  @Autowired private NotebooksService notebooksService;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, NotebooksServiceImpl.class, FirecloudMapperImpl.class})
  static class Configuration {

    @Bean
    Clock clock() {
      return new FakeClock();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return dbUser;
    }
  }

  @BeforeEach
  public void setup() {
    dbUser = new DbUser();
    dbUser.setUserId(101L);
    dbUser.setUsername("panic@thedis.co");
    dbUser = userDao.save(dbUser);

    // workspaceDao is a mock, so we don't need to save the workspace
    dbWorkspace = new DbWorkspace();
    DbCdrVersion cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion = cdrVersionDao.save(cdrVersion);
    dbWorkspace.setCdrVersion(cdrVersion);

    fromCDRVersion = new DbCdrVersion();
    toCDRVersion = new DbCdrVersion();
    fromAccessTier = new DbAccessTier();
    toAccessTier = new DbAccessTier();
  }

  @Mock private Blob mockBlob;

  private void stubGetWorkspace(
      String workspaceNamespace,
      String workspaceName,
      String bucketName,
      WorkspaceAccessLevel access) {
    when(mockFireCloudService.getWorkspace(workspaceNamespace, workspaceName))
        .thenReturn(
            new RawlsWorkspaceResponse()
                .accessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access))
                .workspace(
                    new RawlsWorkspaceDetails()
                        .namespace(workspaceNamespace)
                        .name(workspaceName)
                        .bucketName(bucketName)));
  }

  private void stubNotebookToJson() {
    when(mockFireCloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(
            new RawlsWorkspaceResponse().workspace(new RawlsWorkspaceDetails().bucketName("bkt")));
    when(mockBlob.getContent()).thenReturn("{}".getBytes());
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
  }

  @Test
  public void testAdminGetReadOnlyHtml() {
    RawlsWorkspaceDetails firecloudWorkspaceDetails = new RawlsWorkspaceDetails();
    firecloudWorkspaceDetails.setBucketName(BUCKET_NAME);
    RawlsWorkspaceResponse firecloudWorkspaceResponse = new RawlsWorkspaceResponse();
    firecloudWorkspaceResponse.setWorkspace(firecloudWorkspaceDetails);
    when(mockFireCloudService.getWorkspaceAsService(anyString(), anyString()))
        .thenReturn(firecloudWorkspaceResponse);

    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    String htmlDocument = "<body><div>test</div></body>";

    when(mockFireCloudService.staticNotebooksConvert(any())).thenReturn(htmlDocument);

    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockBlob.getContent()).thenReturn(new byte[10]);
    String actualResult =
        notebooksService.adminGetReadOnlyHtml(NAMESPACE_NAME, WORKSPACE_NAME, "notebookName.ipynb");
    assertThat(actualResult).isEqualTo(htmlDocument);
  }

  @Test
  public void testAdminGetReadOnlyHtml_requiresFileSuffix() {
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            notebooksService.adminGetReadOnlyHtml(NAMESPACE_NAME, WORKSPACE_NAME, "notebookName"));
  }

  @Test
  public void testCloneNotebook() {
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    // Does not verify the response of clone because it is essentially the same as copyNotebook,
    // and that is tested below.
    notebooksService.cloneNotebook(NAMESPACE_NAME, WORKSPACE_NAME, PREVIOUS_NOTEBOOK);

    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.READER);
    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockWorkspaceAuthService).validateActiveBilling(NAMESPACE_NAME, WORKSPACE_NAME);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_CLONE);
  }

  @Test
  public void testCopyNotebook() {
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromNotebookName = "fromNotebookName.ipynb";
    String fromBucket = "FROM_BUCKET";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newNotebookName = "newNotebookName.ipynb";
    String toBucket = "TO_BUCKET";
    HashSet<BlobId> existingBlobIds = new HashSet<>();

    fromCDRVersion.setAccessTier(fromAccessTier);
    toCDRVersion.setAccessTier(toAccessTier);

    stubGetWorkspace(
        fromWorkspaceNamespace, fromWorkspaceFirecloudName, fromBucket, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(
        toWorkspaceNamespace, toWorkspaceFirecloudName, toBucket, WorkspaceAccessLevel.OWNER);

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);
    when(workspaceDao.getRequired(fromWorkspaceNamespace, fromWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);
    when(workspaceDao.getRequired(toWorkspaceNamespace, toWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);
    when(mockCloudStorageClient.getExistingBlobIdsIn(any())).thenReturn(existingBlobIds);

    FileDetail actualFileDetail =
        notebooksService.copyNotebook(
            fromWorkspaceNamespace,
            fromWorkspaceFirecloudName,
            fromNotebookName,
            toWorkspaceNamespace,
            toWorkspaceFirecloudName,
            newNotebookName);

    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(
            fromWorkspaceNamespace, fromWorkspaceFirecloudName, WorkspaceAccessLevel.READER);
    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(
            toWorkspaceNamespace, toWorkspaceFirecloudName, WorkspaceAccessLevel.WRITER);
    verify(mockWorkspaceAuthService)
        .validateActiveBilling(toWorkspaceNamespace, toWorkspaceFirecloudName);

    FileDetail expectedFileDetail =
        new FileDetail()
            .name(newNotebookName)
            .path("gs://" + toBucket + "/notebooks/" + newNotebookName);
    assertThat(actualFileDetail.getName()).isEqualTo(expectedFileDetail.getName());
    assertThat(actualFileDetail.getPath()).isEqualTo(expectedFileDetail.getPath());
    assertThat(actualFileDetail.getSizeInBytes()).isEqualTo(expectedFileDetail.getSizeInBytes());
  }

  @Test
  public void testCopyNotebook_fromDifferentTiers() {
    DbWorkspace fromWorkSpace = new DbWorkspace();
    DbWorkspace toWorkSpace = new DbWorkspace();
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromNotebookName = "fromNotebookName";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newNotebookName = "newNotebookName";

    fromAccessTier.setDisplayName("A Tier");
    toAccessTier.setDisplayName("B Tier");
    fromCDRVersion.setAccessTier(fromAccessTier);
    toCDRVersion.setAccessTier(toAccessTier);
    fromWorkSpace.setCdrVersion(fromCDRVersion);
    toWorkSpace.setCdrVersion(toCDRVersion);

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(fromWorkspaceNamespace, fromWorkspaceFirecloudName))
        .thenReturn(fromWorkSpace);
    when(workspaceDao.getRequired(toWorkspaceNamespace, toWorkspaceFirecloudName))
        .thenReturn(toWorkSpace);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                notebooksService.copyNotebook(
                    fromWorkspaceNamespace,
                    fromWorkspaceFirecloudName,
                    fromNotebookName,
                    toWorkspaceNamespace,
                    toWorkspaceFirecloudName,
                    newNotebookName));
    assertThat(exception.getMessage())
        .isEqualTo("Cannot copy between access tiers (attempted copy from A Tier to B Tier)");
  }

  @Test
  public void testCopyNotebook_alreadyExists() {
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromNotebookName = "fromNotebookName";
    String fromBucket = "FROM_BUCKET";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newNotebookName = "newNotebookName";
    String toBucket = "TO_BUCKET";
    HashSet<BlobId> existingBlobIds = new HashSet<>();

    stubGetWorkspace(
        fromWorkspaceNamespace, fromWorkspaceFirecloudName, fromBucket, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(
        toWorkspaceNamespace, toWorkspaceFirecloudName, toBucket, WorkspaceAccessLevel.OWNER);

    existingBlobIds.add(mockBlob.getBlobId());

    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());
    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(fromWorkspaceNamespace, fromWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);
    when(workspaceDao.getRequired(toWorkspaceNamespace, toWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);

    when(mockCloudStorageClient.getExistingBlobIdsIn(any())).thenReturn(existingBlobIds);

    assertThrows(
        BlobAlreadyExistsException.class,
        () ->
            notebooksService.copyNotebook(
                fromWorkspaceNamespace,
                fromWorkspaceFirecloudName,
                fromNotebookName,
                toWorkspaceNamespace,
                toWorkspaceFirecloudName,
                newNotebookName));
  }

  @Test
  public void testDeleteNotebook() {
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    notebooksService.deleteNotebook(NAMESPACE_NAME, WORKSPACE_NAME, NOTEBOOK_NAME);
    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_DELETE);
  }

  @Test
  public void testGetNotebookContents() {
    JSONObject expectedResult = new JSONObject();
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(expectedResult);

    JSONObject actualResult = notebooksService.getNotebookContents(BUCKET_NAME, "notebookName");
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @Test
  public void testGetNotebookContents_tooBig() {
    JSONObject expectedResult = new JSONObject();
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    // The current max notebook read size in bytes is 5e6 or 5mb.
    when(mockBlob.getSize()).thenReturn((long) 5e6);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(expectedResult);

    Exception exception =
        assertThrows(
            FailedPreconditionException.class,
            () -> notebooksService.getNotebookContents(BUCKET_NAME, "notebookName"));
    assertThat(exception.getMessage())
        .isEqualTo("target notebook is too large to process @ 5.00MB");
  }

  @Test
  public void testGetNotebookKernel_exception() {
    JSONObject notebookFile = new JSONObject();
    KernelTypeEnum kernelType = notebooksService.getNotebookKernel(notebookFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetNotebookKernel_fromBucket() {
    JSONObject notebookFile = new JSONObject();

    stubGetWorkspace(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        BUCKET_NAME,
        WorkspaceAccessLevel.OWNER);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(notebookFile);
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);

    KernelTypeEnum kernelType =
        notebooksService.getNotebookKernel(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), NOTEBOOK_NAME);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetNotebookKernel_python() {
    JSONObject notebookFile = new JSONObject();
    JSONObject kernelSpec = new JSONObject();
    JSONObject language = new JSONObject();

    language.put("language", "Python");
    kernelSpec.put("kernelspec", language);
    notebookFile.put("metadata", kernelSpec);

    KernelTypeEnum kernelType = notebooksService.getNotebookKernel(notebookFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetNotebookKernel_r() {
    JSONObject notebookFile = new JSONObject();
    JSONObject kernelSpec = new JSONObject();
    JSONObject language = new JSONObject();

    language.put("language", "R");
    kernelSpec.put("kernelspec", language);
    notebookFile.put("metadata", kernelSpec);

    KernelTypeEnum kernelType = notebooksService.getNotebookKernel(notebookFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.R);
  }

  @Test
  public void testGetNotebooks() {
    MockNotebook notebook1 =
        MockNotebook.mockWithPathAndJupyterExtension(
            "mockFile", BUCKET_NAME, mockCloudStorageClient);
    MockNotebook notebook2 =
        MockNotebook.mockWithPath("mockFile.text", BUCKET_NAME, mockCloudStorageClient);
    MockNotebook notebook3 =
        MockNotebook.mockWithPathAndJupyterExtension(
            "two words", BUCKET_NAME, mockCloudStorageClient);

    when(mockCloudStorageClient.getBlobPageForPrefix(
            BUCKET_NAME, NotebookUtils.NOTEBOOKS_WORKSPACE_DIRECTORY))
        .thenReturn(ImmutableList.of(notebook1.blob, notebook2.blob, notebook3.blob));
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    List<FileDetail> gotNames =
        notebooksService.getNotebooks(NAMESPACE_NAME, WORKSPACE_NAME).stream()
            .collect(Collectors.toList());

    // Note that notebook 2 is not included because it is not actually a notebook file but a text
    // file.
    assertThat(gotNames).isEqualTo(ImmutableList.of(notebook1.fileDetail, notebook3.fileDetail));
  }

  @Test
  public void testGetNotebooks_notFound() {
    when(mockFireCloudService.getWorkspace("mockProject", "mockWorkspace"))
        .thenThrow(new org.pmiops.workbench.exceptions.NotFoundException());
    assertThrows(
        org.pmiops.workbench.exceptions.NotFoundException.class,
        () -> notebooksService.getNotebooks("mockProject", "mockWorkspace"));
  }

  @Test
  public void testGetNotebooks_mixedFileTypes() {
    Blob mockBlob1 = mock(Blob.class);
    Blob mockBlob2 = mock(Blob.class);
    Blob mockBlob3 = mock(Blob.class);
    FileDetail fileDetail1 = mock(FileDetail.class);
    FileDetail fileDetail2 = mock(FileDetail.class);
    FileDetail fileDetail3 = mock(FileDetail.class);
    Set<String> workspaceUsersSet = new HashSet<String>();

    stubGetWorkspace(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        BUCKET_NAME,
        WorkspaceAccessLevel.OWNER);
    when(mockBlob1.getName()).thenReturn(NotebookUtils.withNotebookPath("f1.ipynb"));
    when(mockBlob2.getName()).thenReturn(NotebookUtils.withNotebookPath("f2.Rmd"));
    when(mockBlob3.getName()).thenReturn(NotebookUtils.withNotebookPath("f3.random"));
    when(mockCloudStorageClient.getBlobPageForPrefix(
            BUCKET_NAME, NotebookUtils.NOTEBOOKS_WORKSPACE_DIRECTORY))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2, mockBlob3));
    when(mockCloudStorageClient.blobToFileDetail(mockBlob1, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail1);
    when(mockCloudStorageClient.blobToFileDetail(mockBlob2, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail2);
    when(mockCloudStorageClient.blobToFileDetail(mockBlob3, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail3);
    when(fileDetail1.getName()).thenReturn("f1.ipynb");
    when(fileDetail2.getName()).thenReturn("f2.Rmd");
    when(fileDetail3.getName()).thenReturn("f3.random");

    List<FileDetail> body =
        notebooksService.getNotebooks(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    List<String> gotNames = body.stream().map(FileDetail::getName).collect(Collectors.toList());

    assertThat(gotNames).isEqualTo(ImmutableList.of("f1.ipynb", "f2.Rmd"));
  }

  @Test
  public void testGetNotebooks_omitsExtraDirectories() {
    Blob mockBlob1 = mock(Blob.class);
    Blob mockBlob2 = mock(Blob.class);
    FileDetail fileDetail1 = mock(FileDetail.class);
    FileDetail fileDetail2 = mock(FileDetail.class);
    Set<String> workspaceUsersSet = new HashSet<String>();

    stubGetWorkspace(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        BUCKET_NAME,
        WorkspaceAccessLevel.OWNER);
    when(mockBlob1.getName())
        .thenReturn(
            NotebookUtils.withNotebookPath(
                NotebookUtils.withJupyterNotebookExtension("extra/nope")));
    when(mockBlob2.getName())
        .thenReturn(
            NotebookUtils.withNotebookPath(NotebookUtils.withJupyterNotebookExtension("foo")));
    when(mockCloudStorageClient.getBlobPageForPrefix(
            BUCKET_NAME, NotebookUtils.NOTEBOOKS_WORKSPACE_DIRECTORY))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockCloudStorageClient.blobToFileDetail(mockBlob1, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail1);
    when(mockCloudStorageClient.blobToFileDetail(mockBlob2, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail2);
    when(fileDetail1.getName()).thenReturn("nope.ipynb");
    when(fileDetail2.getName()).thenReturn("foo.ipynb");

    List<FileDetail> body =
        notebooksService.getNotebooks(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    List<String> gotNames = body.stream().map(FileDetail::getName).collect(Collectors.toList());

    assertThat(gotNames)
        .isEqualTo(ImmutableList.of(NotebookUtils.withJupyterNotebookExtension("foo")));
  }

  @Test
  public void testGetReadOnlyHtml_allowsDataImage() {
    stubNotebookToJson();
    String dataUri = "data:image/png;base64,MTIz";
    when(mockFireCloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"" + dataUri + "\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains(dataUri);
  }

  @Test
  public void testGetReadOnlyHtml_basicContent() {
    stubNotebookToJson();
    when(mockFireCloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_disallowsRemoteImage() {
    stubNotebookToJson();
    when(mockFireCloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("eviltrackingpixel.com");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() {
    stubNotebookToJson();
    when(mockFireCloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() {
    stubNotebookToJson();
    when(mockFireCloudService.staticNotebooksConvert(any()))
        .thenReturn(
            "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} div {color: 'red'}</STYLE>\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("style");
    assertThat(html).contains("color");
    // This behavior is not desired, but this test is in place to enshrine current expected
    // behavior. Style tags can introduce vulnerabilities as demonstrated in the test case - we
    // expect that the only style tags produced in the preview are produced by nbconvert, and are
    // therefore safe. Ideally we would keep the style tag, but sanitize the contents.
    assertThat(html).contains("XSS");
  }

  @Test
  public void testGetReadOnlyHtml_tooBig() {
    when(mockBlob.getSize()).thenReturn(50L * 1000 * 1000); // 50MB
    stubNotebookToJson();

    try {
      notebooksService.getReadOnlyHtml("", "", "").getBytes();
      fail("expected 412 exception");
    } catch (FailedPreconditionException e) {
      // expected
    }
    verify(mockFireCloudService, never()).staticNotebooksConvert(any());
  }

  @Test
  public void testIsNotebookBlob_negative() {
    when(mockBlob.getName()).thenReturn(NotebookUtils.withNotebookPath("test.txt"));
    assertThat(notebooksService.isNotebookBlob(mockBlob)).isEqualTo(false);
  }

  @Test
  public void testRenameNotebook() {

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(anyString(), anyString())).thenReturn(dbWorkspace);

    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    FileDetail actualResult =
        notebooksService.renameNotebook(
            NAMESPACE_NAME,
            WORKSPACE_NAME,
            NotebookUtils.withJupyterNotebookExtension("oldName"),
            NotebookUtils.withJupyterNotebookExtension("newName"));

    verify(mockCloudStorageClient).deleteBlob(any());
    verify(mockUserRecentResourceService).deleteNotebookEntry(anyLong(), anyLong(), anyString());
    verify(mockWorkspaceAuthService, times(2))
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockWorkspaceAuthService).validateActiveBilling(NAMESPACE_NAME, WORKSPACE_NAME);
    assertThat(actualResult.getName()).isEqualTo("newName.ipynb");
    assertThat(actualResult.getPath())
        .isEqualTo("gs://" + BUCKET_NAME + "/notebooks/newName.ipynb");
  }

  @Test
  public void testSaveNotebook_firesMetric() {
    notebooksService.saveNotebook(
        BUCKET_NAME, NOTEBOOK_NAME, new JSONObject().put("who", "I'm a notebook!"));
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_SAVE);
  }

  @Test
  public void testSaveNotebook_notSupportedWithoutSuffix() {
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            notebooksService.saveNotebook(
                BUCKET_NAME, "test", new JSONObject().put("who", "I'm a notebook!")));
  }

  @Test
  public void testSaveNotebook_rmdNotSupported() {
    Assertions.assertThrows(
        NotImplementedException.class,
        () ->
            notebooksService.saveNotebook(
                BUCKET_NAME, "test.Rmd", new JSONObject().put("who", "I'm a notebook!")));
  }
}
