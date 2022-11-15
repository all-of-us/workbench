package org.pmiops.workbench.fileArtifacts;

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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.firecloud.FireCloudService;
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
public class FileArtifactsServiceTest {
  private static final String BUCKET_NAME = "BUCKET_NAME";
  private static final String NOTEBOOK_NAME = "my first fileArtifact";
  private static final String NAMESPACE_NAME = "namespace_name";
  private static final String WORKSPACE_NAME = "workspace_name";
  private static final String PREVIOUS_NOTEBOOK = "previous fileArtifact";

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

  @Autowired private FileArtifactsService fileArtifactsService;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, FileArtifactsServiceImpl.class})
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

  class MockFileArtifact {
    Blob blob;
    FileDetail fileDetail;

    MockFileArtifact(String path, String bucketName) {
      blob = mock(Blob.class);
      fileDetail = new FileDetail();
      Set<String> workspaceUsersSet = new HashSet();

      String[] parts = path.split("/");
      String fileName = parts[parts.length - 1];
      fileDetail.setName(fileName);

      when(blob.getName()).thenReturn(path);
      when(mockCloudStorageClient.blobToFileDetail(blob, bucketName, workspaceUsersSet))
          .thenReturn(fileDetail);
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
            new FirecloudWorkspaceResponse()
                .accessLevel(access.toString())
                .workspace(
                    new FirecloudWorkspaceDetails()
                        .namespace(workspaceNamespace)
                        .name(workspaceName)
                        .bucketName(bucketName)));
  }

  private void stubFileArtifactToJson() {
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
    firecloudWorkspaceDetails.setBucketName(BUCKET_NAME);
    FirecloudWorkspaceResponse firecloudWorkspaceResponse = new FirecloudWorkspaceResponse();
    firecloudWorkspaceResponse.setWorkspace(firecloudWorkspaceDetails);
    when(mockFireCloudService.getWorkspaceAsService(anyString(), anyString()))
        .thenReturn(firecloudWorkspaceResponse);

    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    String htmlDocument = "<body><div>test</div></body>";

    when(mockFireCloudService.staticFileArtifactsConvert(any())).thenReturn(htmlDocument);

    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockBlob.getContent()).thenReturn(new byte[10]);
    String actualResult =
        fileArtifactsService.adminGetReadOnlyHtml(NAMESPACE_NAME, WORKSPACE_NAME, "fileArtifactName");
    assertThat(actualResult).isEqualTo(htmlDocument);
  }

  @Test
  public void testCloneFileArtifact() {
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    // Does not verify the response of clone because it is essentially the same as copyFileArtifact,
    // and that is tested below.
    fileArtifactsService.cloneFileArtifact(NAMESPACE_NAME, WORKSPACE_NAME, PREVIOUS_NOTEBOOK);

    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.READER);
    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockWorkspaceAuthService).validateActiveBilling(NAMESPACE_NAME, WORKSPACE_NAME);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_CLONE);
  }

  @Test
  public void testCopyFileArtifact() {
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromFileArtifactName = "fromFileArtifactName";
    String fromBucket = "FROM_BUCKET";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newFileArtifactName = "newFileArtifactName";
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
        fileArtifactsService.copyFileArtifact(
            fromWorkspaceNamespace,
            fromWorkspaceFirecloudName,
            fromFileArtifactName,
            toWorkspaceNamespace,
            toWorkspaceFirecloudName,
            newFileArtifactName);

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
            .name(newFileArtifactName + ".ipynb")
            .path("gs://" + toBucket + "/fileArtifacts/" + newFileArtifactName + ".ipynb");
    assertThat(actualFileDetail.getName()).isEqualTo(expectedFileDetail.getName());
    assertThat(actualFileDetail.getPath()).isEqualTo(expectedFileDetail.getPath());
    assertThat(actualFileDetail.getSizeInBytes()).isEqualTo(expectedFileDetail.getSizeInBytes());
  }

  @Test
  public void testCopyFileArtifact_fromDifferentTiers() {
    DbWorkspace fromWorkSpace = new DbWorkspace();
    DbWorkspace toWorkSpace = new DbWorkspace();
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromFileArtifactName = "fromFileArtifactName";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newFileArtifactName = "newFileArtifactName";

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
                fileArtifactsService.copyFileArtifact(
                    fromWorkspaceNamespace,
                    fromWorkspaceFirecloudName,
                    fromFileArtifactName,
                    toWorkspaceNamespace,
                    toWorkspaceFirecloudName,
                    newFileArtifactName));
    assertThat(exception.getMessage())
        .isEqualTo("Cannot copy between access tiers (attempted copy from A Tier to B Tier)");
  }

  @Test
  public void testCopyFileArtifact_alreadyExists() {
    String fromWorkspaceNamespace = "fromWorkspaceNamespace";
    String fromWorkspaceFirecloudName = "fromWorkspaceFirecloudName";
    String fromFileArtifactName = "fromFileArtifactName";
    String fromBucket = "FROM_BUCKET";
    String toWorkspaceNamespace = "toWorkspaceNamespace";
    String toWorkspaceFirecloudName = "toWorkspaceFirecloudName";
    String newFileArtifactName = "newFileArtifactName";
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
            fileArtifactsService.copyFileArtifact(
                fromWorkspaceNamespace,
                fromWorkspaceFirecloudName,
                fromFileArtifactName,
                toWorkspaceNamespace,
                toWorkspaceFirecloudName,
                newFileArtifactName));
  }

  @Test
  public void testDeleteFileArtifact() {
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);
    doReturn(dbWorkspace).when(workspaceDao).getRequired(anyString(), anyString());

    fileArtifactsService.deleteFileArtifact(NAMESPACE_NAME, WORKSPACE_NAME, NOTEBOOK_NAME);
    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_DELETE);
  }

  @Test
  public void testGetFileArtifactContents() {
    JSONObject expectedResult = new JSONObject();
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(expectedResult);

    JSONObject actualResult = fileArtifactsService.getFileArtifactContents(BUCKET_NAME, "fileArtifactName");
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @Test
  public void testGetFileArtifactContents_tooBig() {
    JSONObject expectedResult = new JSONObject();
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    // The current max fileArtifact read size in bytes is 5e6 or 5mb.
    when(mockBlob.getSize()).thenReturn((long) 5e6);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(expectedResult);

    Exception exception =
        assertThrows(
            FailedPreconditionException.class,
            () -> fileArtifactsService.getFileArtifactContents(BUCKET_NAME, "fileArtifactName"));
    assertThat(exception.getMessage())
        .isEqualTo("target fileArtifact is too large to process @ 5.00MB");
  }

  @Test
  public void testGetFileArtifactKernel_exception() {
    JSONObject fileArtifactFile = new JSONObject();
    KernelTypeEnum kernelType = fileArtifactsService.getFileArtifactKernel(fileArtifactFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetFileArtifactKernel_fromBucket() {
    JSONObject fileArtifactFile = new JSONObject();

    stubGetWorkspace(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        BUCKET_NAME,
        WorkspaceAccessLevel.OWNER);
    when(mockCloudStorageClient.readBlobAsJson(any())).thenReturn(fileArtifactFile);
    when(mockCloudStorageClient.getBlob(anyString(), anyString())).thenReturn(mockBlob);
    when(mockBlob.getSize()).thenReturn(1l);

    KernelTypeEnum kernelType =
        fileArtifactsService.getFileArtifactKernel(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), NOTEBOOK_NAME);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetFileArtifactKernel_python() {
    JSONObject fileArtifactFile = new JSONObject();
    JSONObject kernelSpec = new JSONObject();
    JSONObject language = new JSONObject();

    language.put("language", "Python");
    kernelSpec.put("kernelspec", language);
    fileArtifactFile.put("metadata", kernelSpec);

    KernelTypeEnum kernelType = fileArtifactsService.getFileArtifactKernel(fileArtifactFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.PYTHON);
  }

  @Test
  public void testGetFileArtifactKernel_r() {
    JSONObject fileArtifactFile = new JSONObject();
    JSONObject kernelSpec = new JSONObject();
    JSONObject language = new JSONObject();

    language.put("language", "R");
    kernelSpec.put("kernelspec", language);
    fileArtifactFile.put("metadata", kernelSpec);

    KernelTypeEnum kernelType = fileArtifactsService.getFileArtifactKernel(fileArtifactFile);
    assertThat(kernelType).isEqualTo(KernelTypeEnum.R);
  }

  @Test
  public void testGetFileArtifacts() {
    MockFileArtifact fileArtifact1 =
        new MockFileArtifact(FileArtifactsService.withFileArtifactExtension("fileArtifacts/mockFile"), BUCKET_NAME);
    MockFileArtifact fileArtifact2 = new MockFileArtifact("fileArtifacts/mockFile.text", BUCKET_NAME);
    MockFileArtifact fileArtifact3 =
        new MockFileArtifact(
            FileArtifactsService.withFileArtifactExtension("fileArtifacts/two words"), BUCKET_NAME);

    when(mockCloudStorageClient.getBlobPageForPrefix(BUCKET_NAME, "fileArtifacts"))
        .thenReturn(ImmutableList.of(fileArtifact1.blob, fileArtifact2.blob, fileArtifact3.blob));
    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    List<FileDetail> gotNames =
        fileArtifactsService.getFileArtifacts(NAMESPACE_NAME, WORKSPACE_NAME).stream()
            .collect(Collectors.toList());

    // Note that fileArtifact 2 is not included because it is not actually a fileArtifact file but a text
    // file.
    assertThat(gotNames).isEqualTo(ImmutableList.of(fileArtifact1.fileDetail, fileArtifact3.fileDetail));
  }

  @Test
  public void testGetFileArtifacts_notFound() {
    when(mockFireCloudService.getWorkspace("mockProject", "mockWorkspace"))
        .thenThrow(new org.pmiops.workbench.exceptions.NotFoundException());
    assertThrows(
        org.pmiops.workbench.exceptions.NotFoundException.class,
        () -> fileArtifactsService.getFileArtifacts("mockProject", "mockWorkspace"));
  }

  @Test
  public void testGetFileArtifacts_omitsExtraDirectories() {
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
        .thenReturn(FileArtifactsService.withFileArtifactExtension("fileArtifacts/extra/nope"));
    when(mockBlob2.getName()).thenReturn(FileArtifactsService.withFileArtifactExtension("fileArtifacts/foo"));
    when(mockCloudStorageClient.getBlobPageForPrefix(BUCKET_NAME, "fileArtifacts"))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockCloudStorageClient.blobToFileDetail(mockBlob1, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail1);
    when(mockCloudStorageClient.blobToFileDetail(mockBlob2, BUCKET_NAME, workspaceUsersSet))
        .thenReturn(fileDetail2);
    when(fileDetail1.getName()).thenReturn("nope.ipynb");
    when(fileDetail2.getName()).thenReturn("foo.ipynb");

    List<FileDetail> body =
        fileArtifactsService.getFileArtifacts(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    List<String> gotNames = body.stream().map(FileDetail::getName).collect(Collectors.toList());

    assertThat(gotNames).isEqualTo(ImmutableList.of(FileArtifactsService.withFileArtifactExtension("foo")));
  }

  @Test
  public void testGetReadOnlyHtml_allowsDataImage() {
    stubFileArtifactToJson();
    String dataUri = "data:image/png;base64,MTIz";
    when(mockFireCloudService.staticFileArtifactsConvert(any()))
        .thenReturn("<img src=\"" + dataUri + "\" />\n");

    String html = new String(fileArtifactsService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains(dataUri);
  }

  @Test
  public void testGetReadOnlyHtml_basicContent() {
    stubFileArtifactToJson();
    when(mockFireCloudService.staticFileArtifactsConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(fileArtifactsService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_disallowsRemoteImage() {
    stubFileArtifactToJson();
    when(mockFireCloudService.staticFileArtifactsConvert(any()))
        .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n");

    String html = new String(fileArtifactsService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("eviltrackingpixel.com");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() {
    stubFileArtifactToJson();
    when(mockFireCloudService.staticFileArtifactsConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(fileArtifactsService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() {
    stubFileArtifactToJson();
    when(mockFireCloudService.staticFileArtifactsConvert(any()))
        .thenReturn(
            "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} div {color: 'red'}</STYLE>\n");

    String html = new String(fileArtifactsService.getReadOnlyHtml("", "", "").getBytes());
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
    stubFileArtifactToJson();

    try {
      fileArtifactsService.getReadOnlyHtml("", "", "").getBytes();
      fail("expected 412 exception");
    } catch (FailedPreconditionException e) {
      // expected
    }
    verify(mockFireCloudService, never()).staticFileArtifactsConvert(any());
  }

  @Test
  public void testIsFileArtifactBlob_negative() {
    when(mockBlob.getName()).thenReturn("fileArtifacts/test.txt");
    assertThat(fileArtifactsService.isFileArtifactBlob(mockBlob)).isEqualTo(false);
  }

  @Test
  public void testRenameFileArtifact() {

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(anyString(), anyString())).thenReturn(dbWorkspace);

    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    FileDetail actualResult =
        fileArtifactsService.renameFileArtifact(
            NAMESPACE_NAME,
            WORKSPACE_NAME,
            FileArtifactsService.withFileArtifactExtension("oldName"),
            FileArtifactsService.withFileArtifactExtension("newName"));

    verify(mockCloudStorageClient).deleteBlob(any());
    verify(mockUserRecentResourceService).deleteFileArtifactEntry(anyLong(), anyLong(), anyString());
    verify(mockWorkspaceAuthService, times(2))
        .enforceWorkspaceAccessLevel(NAMESPACE_NAME, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER);
    verify(mockWorkspaceAuthService).validateActiveBilling(NAMESPACE_NAME, WORKSPACE_NAME);
    assertThat(actualResult.getName()).isEqualTo("newName.ipynb");
    assertThat(actualResult.getPath())
        .isEqualTo("gs://" + BUCKET_NAME + "/fileArtifacts/newName.ipynb");
  }

  @Test
  public void testRenameFileArtifact_withOutExtension() {

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);

    when(workspaceDao.getRequired(anyString(), anyString())).thenReturn(dbWorkspace);

    stubGetWorkspace(NAMESPACE_NAME, WORKSPACE_NAME, BUCKET_NAME, WorkspaceAccessLevel.OWNER);

    FileDetail actualResult =
        fileArtifactsService.renameFileArtifact(NAMESPACE_NAME, WORKSPACE_NAME, "oldName", "newName");

    assertThat(actualResult.getName()).isEqualTo("newName.ipynb");
    assertThat(actualResult.getPath())
        .isEqualTo("gs://" + BUCKET_NAME + "/fileArtifacts/newName.ipynb");
  }

  @Test
  public void testSaveFileArtifact_firesMetric() {
    fileArtifactsService.saveFileArtifact(
        BUCKET_NAME, NOTEBOOK_NAME, new JSONObject().put("who", "I'm a fileArtifact!"));
    verify(mockLogsBasedMetricsService).recordEvent(EventMetric.NOTEBOOK_SAVE);
  }
}
