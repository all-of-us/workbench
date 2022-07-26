package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final String BUCKET_URI =
      String.format("gs://%s/", TestMockFactory.WORKSPACE_BUCKET_NAME);

  @TestConfiguration
  @Import({
    AccessTierServiceImpl.class,
    FakeClockConfiguration.class,
    LogsBasedMetricServiceFakeImpl.class,
    NotebooksController.class,
    NotebooksServiceImpl.class,
    WorkspaceAuthService.class
  })
  @MockBean({CloudStorageClient.class, FireCloudService.class, UserRecentResourceService.class})
  static class Configuration {
    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }
  }

  private static DbUser currentUser;

  private FirecloudWorkspaceACL fcWorkspaceAcl;
  private DbCdrVersion cdrVersion;

  @MockBean private NotebooksService mockNotebooksService;

  @Autowired private CloudStorageClient mockCloudStorageClient;
  @Autowired private FireCloudService mockFireCloudService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @Autowired private NotebooksController notebooksController;

  @BeforeEach
  public void setUp() {
    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    fcWorkspaceAcl = createWorkspaceACL();

    cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao, 1);
    cdrVersion.setName("1");
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(TestMockFactory.createRegisteredTierForTests(accessTierDao));
    cdrVersion = cdrVersionDao.save(cdrVersion);

    when(mockCloudStorageClient.blobToFileDetail(any(), anyString(), anySet()))
        .thenReturn(new FileDetail());
  }

  @AfterEach
  public void tearDown() {
    workspaceDao.deleteAll();
    userDao.deleteAll();
    cdrVersionDao.deleteAll();
    accessTierDao.deleteAll();
  }

  @Test
  public void testNotebookFileList() {
    notebooksController.getNoteBookList("project", "workspace");
    verify(mockNotebooksService).getNotebooks("project", "workspace");
  }

  @Test
  public void testRenameNotebook() {
    NotebookRename rename = new NotebookRename();
    rename.setName("nb1.ipynb");
    rename.setNewName("nb2.ipynb");
    notebooksController.renameNotebook("workspaceNameSpace", "workspaceName", rename);
    verify(mockNotebooksService)
        .renameNotebook(
            "workspaceNameSpace", "workspaceName", rename.getName(), rename.getNewName());
  }

  @Test
  public void testCopyNotebook() {
    DbWorkspace fromWorkspace = createWorkspace();
    String fromNotebookName = "origin";

    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = "new";
    String expectedNotebookName = newNotebookName + NotebooksService.NOTEBOOK_EXTENSION;

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.OWNER);

    CopyRequest copyRequest =
        new CopyRequest()
            .toWorkspaceName(toWorkspace.getFirecloudName())
            .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
            .newName(newNotebookName);

    notebooksController.copyNotebook(
        fromWorkspace.getWorkspaceNamespace(),
        fromWorkspace.getFirecloudName(),
        fromNotebookName,
        copyRequest);

    verify(mockNotebooksService)
        .copyNotebook(
            fromWorkspace.getWorkspaceNamespace(),
            fromWorkspace.getFirecloudName(),
            fromNotebookName + ".ipynb",
            copyRequest.getToWorkspaceNamespace(),
            copyRequest.getToWorkspaceName(),
            newNotebookName + ".ipynb");
  }

  @Test
  public void testCloneNotebook() {
    DbWorkspace workspace = createWorkspace();

    notebooksController.cloneNotebook(
        workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), "nb1.ipynb");
    verify(mockNotebooksService)
        .cloneNotebook(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), "nb1.ipynb");
  }

  @Test
  public void testDeleteNotebook() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    String nb1 = NotebooksService.withNotebookExtension("notebooks/nb1");
    notebooksController.deleteNotebook(
        workspace.getWorkspaceNamespace(),
        workspace.getFirecloudName(),
        NotebooksService.withNotebookExtension("nb1"));
    verify(mockNotebooksService)
        .deleteNotebook(
            eq(workspace.getWorkspaceNamespace()),
            eq(workspace.getFirecloudName()),
            eq(NotebooksService.withNotebookExtension("nb1")));
  }

  private static Stream<Arguments> notebookLockingCases() {
    return Stream.of(
        Arguments.of(
            "fc-bucket-id-1",
            "user@aou",
            "dc5acd54f734a2e2350f2adcb0a25a4d1978b45013b76d6bc0a2d37d035292fe"),
        Arguments.of(
            "fc-bucket-id-1",
            "another-user@aou",
            "bc90f9f740702e5e0408f2ea13fed9457a7ee9c01117820f5c541067064468c3"),
        Arguments.of(
            "fc-bucket-id-2",
            "user@aou",
            "a759e5aef091fd22bbf40bf8ee7cfde4988c668541c18633bd79ab84b274d622"),
        // catches an edge case where the hash has a leading 0
        Arguments.of(
            "fc-5ac6bde3-f225-44ca-ad4d-92eed68df7db",
            "brubenst2@fake-research-aou.org",
            "060c0b2ef2385804b7b69a4b4477dd9661be35db270c940525c2282d081aef56"));
  }

  @ParameterizedTest
  @MethodSource("notebookLockingCases")
  public void notebookLockingEmailHashTest(String bucket, String email, String hash) {
    assertThat(notebooksController.notebookLockingEmailHash(bucket, email)).isEqualTo(hash);
  }

  @Test
  public void testNotebookLockingMetadataPlaintextUser() {
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
  public void testNotebookLockingNullMetadata() {
    final Map<String, String> gcsMetadata = null;

    // This file has no metadata so the response is empty

    final NotebookLockingMetadataResponse expectedResponse = new NotebookLockingMetadataResponse();
    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testNotebookLockingEmptyMetadata() {
    final Map<String, String> gcsMetadata = new HashMap<>();

    // This file has no metadata so the response is empty

    final NotebookLockingMetadataResponse expectedResponse = new NotebookLockingMetadataResponse();
    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  private void assertNotebookLockingMetadata(
      Map<String, String> gcsMetadata,
      NotebookLockingMetadataResponse expectedResponse,
      FirecloudWorkspaceACL acl) {

    final String testWorkspaceNamespace = "test-ns";
    final String testWorkspaceName = "test-ws";
    final String testNotebook = NotebooksService.withNotebookExtension("test-notebook");

    FirecloudWorkspaceDetails fcWorkspace =
        TestMockFactory.createFirecloudWorkspace(
            testWorkspaceNamespace, testWorkspaceName, LOGGED_IN_USER_EMAIL);
    fcWorkspace.setBucketName(TestMockFactory.WORKSPACE_BUCKET_NAME);
    stubGetWorkspace(fcWorkspace, WorkspaceAccessLevel.OWNER);
    stubFcGetWorkspaceACL(acl);

    final String testNotebookPath = "notebooks/" + testNotebook;
    doReturn(gcsMetadata)
        .when(mockCloudStorageClient)
        .getMetadata(TestMockFactory.WORKSPACE_BUCKET_NAME, testNotebookPath);

    assertThat(
            notebooksController
                .getNotebookLockingMetadata(testWorkspaceNamespace, testWorkspaceName, testNotebook)
                .getBody())
        .isEqualTo(expectedResponse);
  }

  @Test
  public void testNotebookLockingMetadata() {
    final String lastLockedUser = LOGGED_IN_USER_EMAIL;
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                notebooksController.notebookLockingEmailHash(
                    TestMockFactory.WORKSPACE_BUCKET_NAME, lastLockedUser))
            .put("extraMetadata", "is not a problem")
            .build();

    // I can see that I have locked it myself, and when

    final NotebookLockingMetadataResponse expectedResponse =
        new NotebookLockingMetadataResponse()
            .lockExpirationTime(lockExpirationTime)
            .lastLockedBy(lastLockedUser);

    assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl);
  }

  @Test
  public void testNotebookLockingMetadataKnownUser() {
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
                notebooksController.notebookLockingEmailHash(
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
  public void testNotebookLockingMetadataUnknownUser() {
    final String lastLockedUser = "a-stranger@fake-research-aou.org";
    final Long lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli();

    final Map<String, String> gcsMetadata =
        new ImmutableMap.Builder<String, String>()
            .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
            .put(
                LAST_LOCKING_USER_KEY,
                notebooksController.notebookLockingEmailHash(
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

  private DbWorkspace newWorkspace() {
    return new DbWorkspace()
        .setName("name")
        .setCreator(currentUser)
        .setFirecloudName("fc-name")
        .setWorkspaceNamespace("namespace")
        .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE)
        .setCdrVersion(cdrVersion)
        .setGoogleProject("proj");
  }

  private DbWorkspace createWorkspace() {
    return createWorkspace(newWorkspace());
  }

  private DbWorkspace createWorkspace(String namespace, String name) {
    return createWorkspace(
        newWorkspace()
            .setWorkspaceNamespace(namespace)
            .setName(name)
            .setFirecloudName("fc-" + name));
  }

  private DbWorkspace createWorkspace(DbWorkspace workspace) {
    assertWithMessage("test code issue: name and firecloudName should be distinct")
        .that(workspace.getFirecloudName())
        .isNotEqualTo(workspace.getName());
    return workspaceDao.save(workspace);
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

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setDisabled(false);
    return userDao.save(user);
  }
}
