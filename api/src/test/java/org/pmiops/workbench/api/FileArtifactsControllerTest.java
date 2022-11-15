package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.fileArtifacts.FileArtifactsService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.KernelTypeResponse;
import org.pmiops.workbench.model.FileArtifactLockingMetadataResponse;
import org.pmiops.workbench.model.FileArtifactRename;
import org.pmiops.workbench.model.ReadOnlyFileArtifactResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.fileArtifacts.FileArtifactLockingUtils;
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
public class FileArtifactsControllerTest {
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String LOCK_EXPIRE_TIME_KEY = "lockExpiresAt";
  private static final String LAST_LOCKING_USER_KEY = "lastLockedBy";
  private static final String FROM_NOTEBOOK_NAME = "starter.ipynb";
  private static final String FROM_WORKSPACE_NAME = "fromWorkspace_000";
  private static final String FROM_WORKSPACE_NAMESPACE = "fromProject";
  private static final String TO_NOTEBOOK_NAME = "novice.ipynb";

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    FakeClockConfiguration.class,
    LogsBasedMetricServiceFakeImpl.class,
    FileArtifactsController.class,
  })
  @MockBean({CloudStorageClient.class, FireCloudService.class, UserRecentResourceService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }
  }

  class MockFileArtifact {
    Blob blob;
    FileDetail fileDetail;

    MockFileArtifact(String path, String bucketName) {
      blob = mock(Blob.class);
      fileDetail = new FileDetail();

      String[] parts = path.split("/");
      String fileName = parts[parts.length - 1];
      fileDetail.setName(fileName);

      when(blob.getName()).thenReturn(path);
      when(mockCloudStorageClient.blobToFileDetail(blob, bucketName, mock(Set.class)))
          .thenReturn(fileDetail);
    }
  }

  private static DbUser currentUser;

  private FirecloudWorkspaceACL fcWorkspaceAcl;

  @MockBean private FileArtifactsService mockFileArtifactService;
  @MockBean private WorkspaceAuthService mockWorkspaceAuthService;

  @Autowired private CloudStorageClient mockCloudStorageClient;
  @Autowired private FireCloudService mockFireCloudService;

  @Autowired private FileArtifactsController fileArtifactsController;

  @BeforeEach
  public void setUp() {
    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    fcWorkspaceAcl = createWorkspaceACL();
  }

  @Test
  public void testCloneFileArtifact() {
    String toFileArtifactName = "Duplicate of starter.ipynb";
    String toPath = "/path/to/" + toFileArtifactName;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    FileDetail expectedFileDetail = createFileDetail(toFileArtifactName, toPath, toLastModifiedTime);

    when(mockFileArtifactService.cloneFileArtifact(anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        fileArtifactsController
            .cloneFileArtifact(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockFileArtifactService)
        .cloneFileArtifact(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCloneFileArtifact_alreadyExists() {
    when(mockFileArtifactService.cloneFileArtifact(anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                fileArtifactsController.cloneFileArtifact(
                    FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME));

    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
    verify(mockFileArtifactService)
        .cloneFileArtifact(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);
  }

  @Test
  public void testCopyFileArtifact() {
    String toWorkspaceNamespace = "fromProject";
    String toWorkspaceName = "fromWorkspace_001";
    String toPath = "/path/to/" + TO_NOTEBOOK_NAME;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    CopyRequest copyRequest = new CopyRequest();
    FileDetail expectedFileDetail = createFileDetail(TO_NOTEBOOK_NAME, toPath, toLastModifiedTime);
    copyRequest.setNewName(TO_NOTEBOOK_NAME);
    copyRequest.setToWorkspaceNamespace(toWorkspaceNamespace);
    copyRequest.setToWorkspaceName(toWorkspaceName);

    when(mockFileArtifactService.copyFileArtifact(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        fileArtifactsController
            .copyFileArtifact(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME, copyRequest)
            .getBody();

    verify(mockFileArtifactService)
        .copyFileArtifact(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            FROM_NOTEBOOK_NAME,
            toWorkspaceNamespace,
            toWorkspaceName,
            TO_NOTEBOOK_NAME);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCopyFileArtifact_alreadyExists() {
    String toWorkspaceNamespace = "fromProject";
    String toWorkspaceName = "fromWorkspace_001";
    CopyRequest copyRequest = new CopyRequest();
    copyRequest.setNewName(TO_NOTEBOOK_NAME);
    copyRequest.setToWorkspaceName(toWorkspaceName);
    copyRequest.setToWorkspaceNamespace(toWorkspaceNamespace);

    when(mockFileArtifactService.copyFileArtifact(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                fileArtifactsController.copyFileArtifact(
                    FROM_WORKSPACE_NAMESPACE,
                    FROM_WORKSPACE_NAME,
                    FROM_NOTEBOOK_NAME,
                    copyRequest));

    verify(mockFileArtifactService)
        .copyFileArtifact(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            FROM_NOTEBOOK_NAME,
            toWorkspaceNamespace,
            toWorkspaceName,
            TO_NOTEBOOK_NAME);
    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
  }

  @Test
  public void testDeleteFileArtifact() {
    String workspaceNamespace = "project";
    String workspaceName = "workspace";
    String fileArtifactName = "fileArtifact.ipynb";

    fileArtifactsController.deleteFileArtifact(workspaceNamespace, workspaceName, fileArtifactName);
    verify(mockFileArtifactService).deleteFileArtifact(workspaceNamespace, workspaceName, fileArtifactName);
  }

  @Test
  public void testGetFileArtifactList() {
    String workspaceNamespace = "project";
    String workspaceName = "workspace";
    MockFileArtifact fileArtifact1 =
        new MockFileArtifact(FileArtifactsService.withFileArtifactExtension("fileArtifacts/mockFile"), "bucket");
    MockFileArtifact fileArtifact2 =
        new MockFileArtifact(FileArtifactsService.withFileArtifactExtension("fileArtifacts/two words"), "bucket");

    when(mockFileArtifactService.getFileArtifacts(anyString(), anyString()))
        .thenReturn(ImmutableList.of(fileArtifact1.fileDetail, fileArtifact2.fileDetail));

    List<FileDetail> actualFileArtifacts =
        fileArtifactsController.getNoteBookList(workspaceNamespace, workspaceName).getBody().stream()
            .collect(Collectors.toList());
    verify(mockFileArtifactService).getFileArtifacts(workspaceNamespace, workspaceName);
    assertThat(actualFileArtifacts)
        .isEqualTo(ImmutableList.of(fileArtifact1.fileDetail, fileArtifact2.fileDetail));
  }

  @Test
  public void testGetFileArtifactKernel() {
    KernelTypeEnum kernelTypeEnum = KernelTypeEnum.PYTHON;
    KernelTypeResponse expectedResponse = new KernelTypeResponse().kernelType(kernelTypeEnum);

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);
    when(mockFileArtifactService.getFileArtifactKernel(anyString(), anyString(), anyString()))
        .thenReturn(kernelTypeEnum);

    KernelTypeResponse actualResponse =
        fileArtifactsController
            .getFileArtifactKernel(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(
            FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, WorkspaceAccessLevel.READER);

    verify(mockFileArtifactService)
        .getFileArtifactKernel(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void testGetFileArtifactLockingMetadata() {
    final String lastLockedUser = LOGGED_IN_USER_EMAIL;
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                FileArtifactLockingUtils.fileArtifactLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    final FileArtifactLockingMetadataResponse expectedResponse =
        new FileArtifactLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy(lastLockedUser);

    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetFileArtifactLockingMetadata_emptyMetadata() {
    final Map<String, String> gcsMetadata = new HashMap<>();

    // This file has no metadata so the response is empty

    final FileArtifactLockingMetadataResponse expectedResponse = new FileArtifactLockingMetadataResponse();
    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetFileArtifactLockingMetadata_knownUser() {
    final String readerOnMyWorkspace = "some-reader@fake-research-aou.org";

    FirecloudWorkspaceACL workspaceACL =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    currentUser.getUsername(),
                    new JSONObject()
                        .put("accessLevel", "OWNER")
                        .put("canCompute", true)
                        .put("canShare", true))
                .put(
                    readerOnMyWorkspace,
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", true)
                        .put("canShare", true)));

    final String lastLockedUser = readerOnMyWorkspace;
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                FileArtifactLockingUtils.fileArtifactLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    // I'm the owner so I can see readers on my workspace

    final FileArtifactLockingMetadataResponse expectedResponse =
        new FileArtifactLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy(readerOnMyWorkspace);

    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, workspaceACL);
  }

  @Test
  public void testGetFileArtifactLockingMetadata_nullMetadata() {
    final Map<String, String> gcsMetadata = null;

    // This file has no metadata so the response is empty

    final FileArtifactLockingMetadataResponse expectedResponse = new FileArtifactLockingMetadataResponse();
    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetFileArtifactLockingMetadata_plaintextUser() {
    final String lastLockedUser = LOGGED_IN_USER_EMAIL;
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            // store directly in plaintext, to show that this does not work
            .put(LAST_LOCKING_USER_KEY, lastLockedUser)
            .put("extraMetadata", "is not a problem")
            .build();

    // in case of accidentally storing the user email in plaintext
    // it can't be retrieved by this endpoint

    final FileArtifactLockingMetadataResponse expectedResponse =
        new FileArtifactLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy("UNKNOWN");

    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetFileArtifactLockingMetadata_unknownUser() {
    final String lastLockedUser = "a-stranger@fake-research-aou.org";
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                FileArtifactLockingUtils.fileArtifactLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    // This user is not listed in the DbWorkspace ACL so I don't know them

    final FileArtifactLockingMetadataResponse expectedResponse =
        new FileArtifactLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy("UNKNOWN");

    assertFileArtifactLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testReadOnlyFileArtifact() {

    String html = "<html><body><div>Hi!</div></body></html>";
    ReadOnlyFileArtifactResponse expectedResponse = new ReadOnlyFileArtifactResponse().html(html);

    when(mockFileArtifactService.getReadOnlyHtml(anyString(), anyString(), anyString()))
        .thenReturn(html);

    ReadOnlyFileArtifactResponse actualResponse =
        fileArtifactsController
            .readOnlyFileArtifact(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockFileArtifactService)
        .getReadOnlyHtml(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void testRenameFileArtifact() {
    String toPath = "/path/to/" + TO_NOTEBOOK_NAME;

    long toLastModifiedTime = Instant.now().toEpochMilli();
    FileArtifactRename fileArtifactRename = new FileArtifactRename();
    FileDetail expectedFileDetail = createFileDetail(TO_NOTEBOOK_NAME, toPath, toLastModifiedTime);

    fileArtifactRename.setName(FROM_NOTEBOOK_NAME);
    fileArtifactRename.setNewName(TO_NOTEBOOK_NAME);

    when(mockFileArtifactService.renameFileArtifact(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        fileArtifactsController
            .renameFileArtifact(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, fileArtifactRename)
            .getBody();

    verify(mockFileArtifactService)
        .renameFileArtifact(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            fileArtifactRename.getName(),
            fileArtifactRename.getNewName());

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testRenameFileArtifact_alreadyExists() {
    FileArtifactRename fileArtifactRename = new FileArtifactRename();

    fileArtifactRename.setName(FROM_NOTEBOOK_NAME);
    fileArtifactRename.setNewName(TO_NOTEBOOK_NAME);

    when(mockFileArtifactService.renameFileArtifact(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                fileArtifactsController.renameFileArtifact(
                    FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, fileArtifactRename));

    verify(mockFileArtifactService)
        .renameFileArtifact(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            fileArtifactRename.getName(),
            fileArtifactRename.getNewName());
    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
  }

  private void assertFileArtifactLockingMetadata(
      Map<String, String> gcsMetadata,
      FileArtifactLockingMetadataResponse expectedResponse,
      FirecloudWorkspaceACL acl) {

    final String testWorkspaceNamespace = "test-ns";
    final String testWorkspaceName = "test-ws";
    final String testFileArtifact = FileArtifactsService.withFileArtifactExtension("test-fileArtifact");

    FirecloudWorkspaceDetails fcWorkspace =
        TestMockFactory.createFirecloudWorkspace(
            testWorkspaceNamespace, testWorkspaceName, LOGGED_IN_USER_EMAIL);
    fcWorkspace.setBucketName(TestMockFactory.WORKSPACE_BUCKET_NAME);
    stubGetWorkspace(fcWorkspace, WorkspaceAccessLevel.OWNER);
    stubFcGetWorkspaceACL(acl);

    when(mockWorkspaceAuthService.getFirecloudWorkspaceAcls(anyString(), anyString()))
        .thenReturn(acl.getAcl());

    final String testFileArtifactPath = "fileArtifacts/" + testFileArtifact;
    doReturn(gcsMetadata)
        .when(mockCloudStorageClient)
        .getMetadata(TestMockFactory.WORKSPACE_BUCKET_NAME, testFileArtifactPath);

    assertThat(
            fileArtifactsController
                .getFileArtifactLockingMetadata(testWorkspaceNamespace, testWorkspaceName, testFileArtifact)
                .getBody())
        .isEqualTo(expectedResponse);
  }

  private FileDetail createFileDetail(String name, String path, long lastModifiedTime) {
    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(name);
    fileDetail.setPath(path);
    fileDetail.setLastModifiedTime(lastModifiedTime);
    return fileDetail;
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setDisabled(false);
    return user;
  }

  private FirecloudWorkspaceACL createWorkspaceACL() {
    return createWorkspaceACL(
        new JSONObject()
            .put(
                currentUser.getUsername(),
                new JSONObject()
                    .put("accessLevel", "OWNER")
                    .put("canCompute", true)
                    .put("canShare", true)));
  }

  private FirecloudWorkspaceACL createWorkspaceACL(JSONObject acl) {
    return new Gson()
        .fromJson(new JSONObject().put("acl", acl).toString(), FirecloudWorkspaceACL.class);
  }

  private void stubFcGetWorkspaceACL(FirecloudWorkspaceACL acl) {
    when(mockFireCloudService.getWorkspaceAclAsService(anyString(), anyString())).thenReturn(acl);
  }

  private void stubGetWorkspace(
      FirecloudWorkspaceDetails fcWorkspace, WorkspaceAccessLevel access) {
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    doReturn(fcResponse)
        .when(mockFireCloudService)
        .getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName());
    List<FirecloudWorkspaceResponse> workspaceResponses = mockFireCloudService.getWorkspaces();
    workspaceResponses.add(fcResponse);
    doReturn(workspaceResponses).when(mockFireCloudService).getWorkspaces();
  }
}
