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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
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
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
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
  private static final JSONObject NOTEBOOK_CONTENTS =
      new JSONObject().put("who", "I'm a notebook!");
  private static final String BUCKET_NAME = "notebook.bucket";
  private static final FirecloudWorkspaceResponse WORKSPACE_RESPONSE =
      new FirecloudWorkspaceResponse()
          .workspace(new FirecloudWorkspaceDetails().bucketName(BUCKET_NAME));
  private static final String NOTEBOOK_NAME = "my first notebook";
  private static final String NAMESPACE_NAME = "namespace_name";
  private static final String WORKSPACE_NAME = "workspace_name";
  private static final String PREVIOUS_NOTEBOOK = "previous notebook";

  private static DbUser dbUser;
  private static DbWorkspace dbWorkspace;

  @MockBean private LogsBasedMetricService mockLogsBasedMetricsService;

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private WorkspaceDao workspaceDao;
  @MockBean private UserRecentResourceService mockUserRecentResourceService;
  @MockBean private WorkspaceAuthService mockWorkspaceAuthService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private UserDao userDao;

  @Autowired private NotebooksService notebooksService;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, NotebooksServiceImpl.class})
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

  class MockNotebook{
    Blob blob;
    FileDetail fileDetail;
    MockNotebook(String path, String bucketName){
      blob = mock(Blob.class);
      fileDetail = new FileDetail();

      String[] parts = path.split("/");
      String fileName = parts[parts.length - 1];
      fileDetail.setName(fileName);

      when(blob.getName())
          .thenReturn(path);
      when(mockCloudStorageClient.blobToFileDetail(blob,bucketName)).thenReturn(fileDetail);

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
    dbWorkspace.setCdrVersion(
        TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao));
  }

  @Mock private Blob mockBlob;

  private void stubGetWorkspace(DbWorkspace workspace, WorkspaceAccessLevel access) {
    when(mockFireCloudService.getWorkspace(
        workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .accessLevel(access.toString())
                .workspace(
                    new FirecloudWorkspaceDetails()
                        .namespace(workspace.getWorkspaceNamespace())
                        .name(workspace.getFirecloudName())
                        .bucketName(TestMockFactory.WORKSPACE_BUCKET_NAME)));
  }

  private void stubNotebookToJson() {
    when(mockFireCloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspaceDetails().bucketName("bkt")));
    when(mockBlob.getContent()).thenReturn("{}".getBytes());
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
  }

  @Test
  public void testAdminGetReadOnlyHtml() {
    FirecloudWorkspaceDetails firecloudWorkspaceDetails = new FirecloudWorkspaceDetails();
    firecloudWorkspaceDetails.setBucketName("bucketName");
    FirecloudWorkspaceResponse firecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    firecloudWorkspaceResponse.setWorkspace(firecloudWorkspaceDetails);
    String htmlDocument = "<body><div>test</div></body>";
    when(mockFireCloudService.getWorkspaceAsService(anyString(), anyString()))
        .thenReturn(firecloudWorkspaceResponse);

    when(mockFireCloudService.staticNotebooksConvert(any())).thenReturn(htmlDocument);

    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockBlob.getContent()).thenReturn(new byte[10]);
    String actualResult =
        notebooksService.adminGetReadOnlyHtml(
            "workspaceNamespace", "workspaceName", "notebookName");
    assertThat(actualResult).isEqualTo(htmlDocument);
  }

  @Test
  public void testCloneNotebook_firesMetric() {
    doReturn(WORKSPACE_RESPONSE).when(mockFireCloudService).getWorkspace(anyString(), anyString());
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    notebooksService.cloneNotebook(NAMESPACE_NAME, WORKSPACE_NAME, PREVIOUS_NOTEBOOK);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_CLONE);
  }

  @Test
  public void testCopyNotebook_fromDifferentTiers() {
    DbWorkspace fromWorkSpace = new DbWorkspace();
    DbWorkspace toWorkSpace = new DbWorkspace();
    DbCdrVersion fromCDRVersion = new DbCdrVersion();
    DbCdrVersion toCDRVersion = new DbCdrVersion();
    DbAccessTier fromAccessTier = new DbAccessTier();
    DbAccessTier toAccessTier = new DbAccessTier();
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
    DbCdrVersion fromCDRVersion = new DbCdrVersion();
    DbCdrVersion toCDRVersion = new DbCdrVersion();
    DbAccessTier fromAccessTier = new DbAccessTier();
    DbAccessTier toAccessTier = new DbAccessTier();
    FirecloudWorkspaceResponse firecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromNotebookName = "fromNotebookName";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newNotebookName = "newNotebookName";
    HashSet<BlobId> existingBlobIds = new HashSet<>();

    fromCDRVersion.setAccessTier(fromAccessTier);
    toCDRVersion.setAccessTier(toAccessTier);
    FirecloudWorkspaceDetails firecloudWorkspaceDetails = new FirecloudWorkspaceDetails();
    firecloudWorkspaceDetails.setBucketName("the_bucket");
    firecloudWorkspaceResponse.setWorkspace(firecloudWorkspaceDetails);
    existingBlobIds.add(mockBlob.getBlobId());

    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());
    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(fromWorkspaceNamespace, fromWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);
    when(workspaceDao.getRequired(toWorkspaceNamespace, toWorkspaceFirecloudName))
        .thenReturn(dbWorkspace);
    when(mockFireCloudService.getWorkspace(any(), any())).thenReturn(firecloudWorkspaceResponse);

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
  public void testDeleteNotebook_firesMetric() {
    doReturn(WORKSPACE_RESPONSE).when(mockFireCloudService).getWorkspace(anyString(), anyString());
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    notebooksService.deleteNotebook(NAMESPACE_NAME, WORKSPACE_NAME, NOTEBOOK_NAME);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_DELETE);
  }

  @Test
  public void testGetNotebookContents() {
    JSONObject expectedResult = new JSONObject();
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(expectedResult);

    JSONObject actualResult = notebooksService.getNotebookContents("bucketName", "notebookName");
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
            () -> notebooksService.getNotebookContents("bucketName", "notebookName"));
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

    FirecloudWorkspaceDetails firecloudWorkspaceDetails = new FirecloudWorkspaceDetails();
    firecloudWorkspaceDetails.setBucketName("bucketName");
    FirecloudWorkspaceResponse firecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    firecloudWorkspaceResponse.setWorkspace(firecloudWorkspaceDetails);
    when(mockFireCloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(firecloudWorkspaceResponse);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(notebookFile);
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);

    KernelTypeEnum kernelType =
        notebooksService.getNotebookKernel("workspaceNamespace", "workspaceName", "notebookName");
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
    MockNotebook notebook1 = new MockNotebook(NotebooksService.withNotebookExtension("notebooks/mockFile"),"bucket");
    MockNotebook notebook2 = new MockNotebook("notebooks/mockFile.text","bucket");
    MockNotebook notebook3 = new MockNotebook(NotebooksService.withNotebookExtension("notebooks/two words"),"bucket");

    when(mockCloudStorageClient.getBlobPageForPrefix("bucket", "notebooks"))
        .thenReturn(ImmutableList.of(notebook1.blob, notebook2.blob, notebook3.blob));
    when(mockFireCloudService.getWorkspace("project", "workspace"))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspaceDetails().bucketName("bucket")));

    List<String> gotNames =
        notebooksService.getNotebooks("project", "workspace").stream()
            .map(FileDetail::getName)
            .collect(Collectors.toList());

    assertThat(gotNames)
        .isEqualTo(
            ImmutableList.of(
                notebook1.fileDetail.getName(),
                notebook3.fileDetail.getName()));
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
  public void testGetNotebooks_omitsExtraDirectories() {
    Blob mockBlob1 = mock(Blob.class);
    Blob mockBlob2 = mock(Blob.class);
    FileDetail fileDetail1 = mock(FileDetail.class);
    FileDetail fileDetail2 = mock(FileDetail.class);

    stubGetWorkspace(dbWorkspace, WorkspaceAccessLevel.OWNER);
    when(mockBlob1.getName())
        .thenReturn(NotebooksService.withNotebookExtension("notebooks/extra/nope"));
    when(mockBlob2.getName()).thenReturn(NotebooksService.withNotebookExtension("notebooks/foo"));
    when(mockCloudStorageClient.getBlobPageForPrefix(
        TestMockFactory.WORKSPACE_BUCKET_NAME, "notebooks"))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockCloudStorageClient.blobToFileDetail(
        mockBlob1, TestMockFactory.WORKSPACE_BUCKET_NAME))
        .thenReturn(fileDetail1);
    when(mockCloudStorageClient.blobToFileDetail(
        mockBlob2, TestMockFactory.WORKSPACE_BUCKET_NAME))
        .thenReturn(fileDetail2);
    when(fileDetail1.getName()).thenReturn("nope.ipynb");
    when(fileDetail2.getName()).thenReturn("foo.ipynb");

    List<FileDetail> body =
        notebooksService.getNotebooks(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    List<String> gotNames = body.stream().map(FileDetail::getName).collect(Collectors.toList());

    assertThat(gotNames).isEqualTo(ImmutableList.of(NotebooksService.withNotebookExtension("foo")));
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
    when(mockBlob.getName()).thenReturn("notebooks/test.txt");
    assertThat(notebooksService.isNotebookBlob(mockBlob)).isEqualTo(false);
  }

  @Test
  public void testRenameNotebook() {

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(anyString(), anyString())).thenReturn(dbWorkspace);

    when(mockFireCloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspaceDetails().bucketName("bkt")));

    FileDetail actualResult =
        notebooksService.renameNotebook(
            "fromWorkspaceNamespace",
            "fromWorkspaceFirecloudName",
            NotebooksService.withNotebookExtension("oldName"),
            NotebooksService.withNotebookExtension("newName"));

    verify(mockCloudStorageClient).deleteBlob(any());
    verify(mockUserRecentResourceService).deleteNotebookEntry(anyLong(), anyLong(), anyString());
    assertThat(actualResult.getName()).isEqualTo("newName.ipynb");
    assertThat(actualResult.getPath()).isEqualTo("gs://bkt/notebooks/newName.ipynb");
  }

  @Test
  public void testRenameNotebook_withOutExtension() {

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(anyString(), anyString())).thenReturn(dbWorkspace);

    when(mockFireCloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspaceDetails().bucketName("bkt")));

    FileDetail actualResult =
        notebooksService.renameNotebook(
            "fromWorkspaceNamespace", "fromWorkspaceFirecloudName", "oldName", "newName");

    assertThat(actualResult.getName()).isEqualTo("newName.ipynb");
    assertThat(actualResult.getPath()).isEqualTo("gs://bkt/notebooks/newName.ipynb");
  }

  @Test
  public void testSaveNotebook_firesMetric() {
    notebooksService.saveNotebook(BUCKET_NAME, NOTEBOOK_NAME, NOTEBOOK_CONTENTS);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_SAVE);
  }
}
