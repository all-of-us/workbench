package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.KernelTypeResponse;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebookLockingUtils;
import org.pmiops.workbench.notebooks.NotebookUtils;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.MockNotebook;
import org.pmiops.workbench.utils.TestMockFactory;
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
public class NotebooksControllerTest {
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
    FirecloudMapperImpl.class,
    FakeClockConfiguration.class,
    LogsBasedMetricServiceFakeImpl.class,
    NotebooksController.class,
  })
  @MockBean({CloudStorageClient.class, FireCloudService.class, UserRecentResourceService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }
  }

  private static DbUser currentUser;

  private RawlsWorkspaceACL fcWorkspaceAcl;

  @MockBean private NotebooksService mockNotebookService;
  @MockBean private WorkspaceAuthService mockWorkspaceAuthService;

  @Autowired private CloudStorageClient mockCloudStorageClient;
  @Autowired private FireCloudService mockFireCloudService;

  @Autowired private NotebooksController notebooksController;

  @Autowired private FirecloudMapper firecloudMapper;

  @BeforeEach
  public void setUp() {
    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    fcWorkspaceAcl = createWorkspaceACL();
  }

  @Test
  public void testCloneNotebook() {
    String toNotebookName = "Duplicate of starter.ipynb";
    String toPath = "/path/to/" + toNotebookName;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    FileDetail expectedFileDetail = createFileDetail(toNotebookName, toPath, toLastModifiedTime);

    when(mockNotebookService.cloneNotebook(anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .cloneNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockNotebookService)
        .cloneNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCloneNotebook_rmd() {
    String fromNotebookName = "starter.Rmd";
    String toNotebookName = "Duplicate of starter.Rmd";
    String toPath = "/path/to/" + toNotebookName;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    FileDetail expectedFileDetail = createFileDetail(toNotebookName, toPath, toLastModifiedTime);

    when(mockNotebookService.cloneNotebook(anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .cloneNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, fromNotebookName)
            .getBody();

    verify(mockNotebookService)
        .cloneNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, fromNotebookName);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCloneNotebook_alreadyExists() {
    when(mockNotebookService.cloneNotebook(anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                notebooksController.cloneNotebook(
                    FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME));

    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
    verify(mockNotebookService)
        .cloneNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);
  }

  @Test
  public void testCopyNotebook() {
    String toWorkspaceNamespace = "fromProject";
    String toWorkspaceName = "fromWorkspace_001";
    String toPath = "/path/to/" + TO_NOTEBOOK_NAME;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    CopyRequest copyRequest = new CopyRequest();
    FileDetail expectedFileDetail = createFileDetail(TO_NOTEBOOK_NAME, toPath, toLastModifiedTime);
    copyRequest.setNewName(TO_NOTEBOOK_NAME);
    copyRequest.setToWorkspaceNamespace(toWorkspaceNamespace);
    copyRequest.setToWorkspaceName(toWorkspaceName);

    when(mockNotebookService.copyNotebook(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .copyNotebook(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME, copyRequest)
            .getBody();

    verify(mockNotebookService)
        .copyNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            FROM_NOTEBOOK_NAME,
            toWorkspaceNamespace,
            toWorkspaceName,
            TO_NOTEBOOK_NAME);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCopyNotebook_rmd() {
    String fromNotebookName = "test.Rmd";
    String toWorkspaceNamespace = "fromProject";
    String toWorkspaceName = "fromWorkspace_001";
    String newName = "toName";
    String newNameWithExtension = newName + ".Rmd";
    String toPath = "/path/to/" + newName;
    long toLastModifiedTime = Instant.now().toEpochMilli();
    CopyRequest copyRequest = new CopyRequest();
    FileDetail expectedFileDetail =
        createFileDetail(newNameWithExtension, toPath, toLastModifiedTime);
    copyRequest.setNewName(newName);
    copyRequest.setToWorkspaceNamespace(toWorkspaceNamespace);
    copyRequest.setToWorkspaceName(toWorkspaceName);

    when(mockNotebookService.copyNotebook(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .copyNotebook(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, fromNotebookName, copyRequest)
            .getBody();

    verify(mockNotebookService)
        .copyNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            fromNotebookName,
            toWorkspaceNamespace,
            toWorkspaceName,
            newNameWithExtension);

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testCopyNotebook_alreadyExists() {
    String toWorkspaceNamespace = "fromProject";
    String toWorkspaceName = "fromWorkspace_001";
    CopyRequest copyRequest = new CopyRequest();
    copyRequest.setNewName(TO_NOTEBOOK_NAME);
    copyRequest.setToWorkspaceName(toWorkspaceName);
    copyRequest.setToWorkspaceNamespace(toWorkspaceNamespace);

    when(mockNotebookService.copyNotebook(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                notebooksController.copyNotebook(
                    FROM_WORKSPACE_NAMESPACE,
                    FROM_WORKSPACE_NAME,
                    FROM_NOTEBOOK_NAME,
                    copyRequest));

    verify(mockNotebookService)
        .copyNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            FROM_NOTEBOOK_NAME,
            toWorkspaceNamespace,
            toWorkspaceName,
            TO_NOTEBOOK_NAME);
    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
  }

  @Test
  public void testDeleteNotebook() {
    String workspaceNamespace = "project";
    String workspaceName = "workspace";
    String notebookName = "notebook.ipynb";

    notebooksController.deleteNotebook(workspaceNamespace, workspaceName, notebookName);
    verify(mockNotebookService).deleteNotebook(workspaceNamespace, workspaceName, notebookName);
  }

  @Test
  public void testGetNotebookList() {
    String workspaceNamespace = "project";
    String workspaceName = "workspace";
    MockNotebook notebook1 =
        MockNotebook.mockWithPathAndJupyterExtension("mockFile", "bucket", mockCloudStorageClient);
    MockNotebook notebook2 =
        MockNotebook.mockWithPathAndJupyterExtension("two words", "bucket", mockCloudStorageClient);

    when(mockNotebookService.getNotebooks(anyString(), anyString()))
        .thenReturn(ImmutableList.of(notebook1.fileDetail, notebook2.fileDetail));

    List<FileDetail> actualNotebooks =
        notebooksController.getNoteBookList(workspaceNamespace, workspaceName).getBody().stream()
            .collect(Collectors.toList());
    verify(mockNotebookService).getNotebooks(workspaceNamespace, workspaceName);
    assertThat(actualNotebooks)
        .isEqualTo(ImmutableList.of(notebook1.fileDetail, notebook2.fileDetail));
  }

  @Test
  public void testGetNotebookKernel() {
    KernelTypeEnum kernelTypeEnum = KernelTypeEnum.PYTHON;
    KernelTypeResponse expectedResponse = new KernelTypeResponse().kernelType(kernelTypeEnum);

    when(mockWorkspaceAuthService.enforceWorkspaceAccessLevel(anyString(), anyString(), any()))
        .thenReturn(WorkspaceAccessLevel.OWNER);
    when(mockNotebookService.getNotebookKernel(anyString(), anyString(), anyString()))
        .thenReturn(kernelTypeEnum);

    KernelTypeResponse actualResponse =
        notebooksController
            .getNotebookKernel(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockWorkspaceAuthService)
        .enforceWorkspaceAccessLevel(
            FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, WorkspaceAccessLevel.READER);

    verify(mockNotebookService)
        .getNotebookKernel(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void testGetNotebookKernel_notSupportedWithoutSuffix() {
    assertThrows(
        BadRequestException.class,
        () ->
            notebooksController.getNotebookKernel(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, "file"));
  }

  @Test
  public void testGetNotebookKernel_rmdNotSupported() {
    assertThrows(
        BadRequestException.class,
        () ->
            notebooksController.getNotebookKernel(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, "file.Rmd"));
  }

  @Test
  public void testGetNotebookLockingMetadata() {
    final String lastLockedUser = LOGGED_IN_USER_EMAIL;
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                NotebookLockingUtils.notebookLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    final NotebookLockingMetadataResponse expectedResponse =
        new NotebookLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy(lastLockedUser);

    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetNotebookLockingMetadata_emptyMetadata() {
    final Map<String, String> gcsMetadata = new HashMap<>();

    // This file has no metadata so the response is empty

    final NotebookLockingMetadataResponse expectedResponse = new NotebookLockingMetadataResponse();
    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetNotebookLockingMetadata_knownUser() {
    final String readerOnMyWorkspace = "some-reader@fake-research-aou.org";

    RawlsWorkspaceACL workspaceACL =
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
                NotebookLockingUtils.notebookLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    // I'm the owner so I can see readers on my workspace

    final NotebookLockingMetadataResponse expectedResponse =
        new NotebookLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy(readerOnMyWorkspace);

    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, workspaceACL);
  }

  @Test
  public void testGetNotebookLockingMetadata_nullMetadata() {
    final Map<String, String> gcsMetadata = null;

    // This file has no metadata so the response is empty

    final NotebookLockingMetadataResponse expectedResponse = new NotebookLockingMetadataResponse();
    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetNotebookLockingMetadata_plaintextUser() {
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

    final NotebookLockingMetadataResponse expectedResponse =
        new NotebookLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy("UNKNOWN");

    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testGetNotebookLockingMetadata_unknownUser() {
    final String lastLockedUser = "a-stranger@fake-research-aou.org";
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                NotebookLockingUtils.notebookLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    // This user is not listed in the DbWorkspace ACL so I don't know them

    final NotebookLockingMetadataResponse expectedResponse =
        new NotebookLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy("UNKNOWN");

    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testReadOnlyNotebook() {

    String html = "<html><body><div>Hi!</div></body></html>";
    ReadOnlyNotebookResponse expectedResponse = new ReadOnlyNotebookResponse().html(html);

    when(mockNotebookService.getReadOnlyHtml(anyString(), anyString(), anyString()))
        .thenReturn(html);

    ReadOnlyNotebookResponse actualResponse =
        notebooksController
            .readOnlyNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME)
            .getBody();

    verify(mockNotebookService)
        .getReadOnlyHtml(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, FROM_NOTEBOOK_NAME);

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void testReadOnlyNotebook_requiresSuffix() {
    assertThrows(
        NotImplementedException.class,
        () ->
            notebooksController.readOnlyNotebook(
                FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, "notebook without suffix"));
  }

  @Test
  public void testRenameNotebook() {
    String toPath = "/path/to/" + TO_NOTEBOOK_NAME;

    long toLastModifiedTime = Instant.now().toEpochMilli();
    NotebookRename notebookRename = new NotebookRename();
    FileDetail expectedFileDetail = createFileDetail(TO_NOTEBOOK_NAME, toPath, toLastModifiedTime);

    notebookRename.setName(FROM_NOTEBOOK_NAME);
    notebookRename.setNewName(TO_NOTEBOOK_NAME);

    when(mockNotebookService.renameNotebook(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .renameNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, notebookRename)
            .getBody();

    verify(mockNotebookService)
        .renameNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            notebookRename.getName(),
            notebookRename.getNewName());

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testRenameNotebook_rmd() {
    String fromNotebookName = "original.Rmd";
    String toNotebookName = "new.Rmd";
    String toPath = "/path/to/" + toNotebookName;

    long toLastModifiedTime = Instant.now().toEpochMilli();
    NotebookRename notebookRename = new NotebookRename();
    FileDetail expectedFileDetail = createFileDetail(toNotebookName, toPath, toLastModifiedTime);

    notebookRename.setName(fromNotebookName);
    notebookRename.setNewName(toNotebookName);

    when(mockNotebookService.renameNotebook(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedFileDetail);

    FileDetail actualFileDetail =
        notebooksController
            .renameNotebook(FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, notebookRename)
            .getBody();

    verify(mockNotebookService)
        .renameNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            notebookRename.getName(),
            notebookRename.getNewName());

    assertThat(actualFileDetail).isEqualTo(expectedFileDetail);
  }

  @Test
  public void testRenameNotebook_alreadyExists() {
    NotebookRename notebookRename = new NotebookRename();

    notebookRename.setName(FROM_NOTEBOOK_NAME);
    notebookRename.setNewName(TO_NOTEBOOK_NAME);

    when(mockNotebookService.renameNotebook(anyString(), anyString(), anyString(), anyString()))
        .thenThrow(BlobAlreadyExistsException.class);

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                notebooksController.renameNotebook(
                    FROM_WORKSPACE_NAMESPACE, FROM_WORKSPACE_NAME, notebookRename));

    verify(mockNotebookService)
        .renameNotebook(
            FROM_WORKSPACE_NAMESPACE,
            FROM_WORKSPACE_NAME,
            notebookRename.getName(),
            notebookRename.getNewName());
    assertThat(exception.getMessage()).isEqualTo("File already exists at copy destination");
  }

  private void assertNotebookLockingMetadata(
      Map<String, String> gcsMetadata,
      NotebookLockingMetadataResponse expectedResponse,
      RawlsWorkspaceACL acl) {

    final String testWorkspaceNamespace = "test-ns";
    final String testWorkspaceName = "test-ws";
    final String testNotebook = NotebookUtils.withJupyterNotebookExtension("test-notebook");

    RawlsWorkspaceDetails fcWorkspace =
        TestMockFactory.createFirecloudWorkspace(
            testWorkspaceNamespace, testWorkspaceName, LOGGED_IN_USER_EMAIL);
    fcWorkspace.setBucketName(TestMockFactory.WORKSPACE_BUCKET_NAME);
    stubGetWorkspace(fcWorkspace, WorkspaceAccessLevel.OWNER);
    stubFcGetWorkspaceACL(acl);

    when(mockWorkspaceAuthService.getFirecloudWorkspaceAcls(anyString(), anyString()))
        .thenReturn(FirecloudTransforms.extractAclResponse(acl));

    final String testNotebookPath = NotebookUtils.withNotebookPath(testNotebook);
    doReturn(gcsMetadata)
        .when(mockCloudStorageClient)
        .getMetadata(TestMockFactory.WORKSPACE_BUCKET_NAME, testNotebookPath);

    assertThat(
            notebooksController
                .getNotebookLockingMetadata(testWorkspaceNamespace, testWorkspaceName, testNotebook)
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

  private RawlsWorkspaceACL createWorkspaceACL() {
    return createWorkspaceACL(
        new JSONObject()
            .put(
                currentUser.getUsername(),
                new JSONObject()
                    .put("accessLevel", "OWNER")
                    .put("canCompute", true)
                    .put("canShare", true)));
  }

  private RawlsWorkspaceACL createWorkspaceACL(JSONObject acl) {
    return new Gson()
        .fromJson(new JSONObject().put("acl", acl).toString(), RawlsWorkspaceACL.class);
  }

  private void stubFcGetWorkspaceACL(RawlsWorkspaceACL acl) {
    when(mockFireCloudService.getWorkspaceAclAsService(anyString(), anyString())).thenReturn(acl);
  }

  private void stubGetWorkspace(RawlsWorkspaceDetails fcWorkspace, WorkspaceAccessLevel access) {
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access));
    doReturn(fcResponse)
        .when(mockFireCloudService)
        .getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName());

    RawlsWorkspaceListResponse fcListResponse = new RawlsWorkspaceListResponse();
    fcListResponse.setWorkspace(fcWorkspace);
    fcListResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access));
    List<RawlsWorkspaceListResponse> workspaceResponses = mockFireCloudService.getWorkspaces();
    workspaceResponses.add(fcListResponse);
    doReturn(workspaceResponses).when(mockFireCloudService).getWorkspaces();
  }
}
