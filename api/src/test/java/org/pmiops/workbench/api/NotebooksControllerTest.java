package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.BillingStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class NotebooksControllerTest extends SpringTest {
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String LOCK_EXPIRE_TIME_KEY = "lockExpiresAt";
  private static final String LAST_LOCKING_USER_KEY = "lastLockedBy";
  private static final String BUCKET_URI =
      String.format("gs://%s/", TestMockFactory.WORKSPACE_BUCKET_NAME);

  @TestConfiguration
  @Import({
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

  private TestMockFactory testMockFactory;
  private FirecloudWorkspaceACL fcWorkspaceAcl;
  private DbCdrVersion cdrVersion;

  @Autowired private CloudStorageClient mockCloudStorageClient;
  @Autowired private FireCloudService mockFireCloudService;
  @Autowired private UserRecentResourceService mockUserRecentResourceService;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @Autowired private NotebooksController notebooksController;

  @BeforeEach
  public void setUp() {
    testMockFactory = new TestMockFactory();

    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    fcWorkspaceAcl = createWorkspaceACL();

    cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao, 1);
    cdrVersion.setName("1");
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(TestMockFactory.createRegisteredTierForTests(accessTierDao));
    cdrVersion = cdrVersionDao.save(cdrVersion);

    // required to enable the use of default method blobToFileDetail()
    when(mockCloudStorageClient.blobToFileDetail(any(), anyString())).thenCallRealMethod();
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
    when(mockFireCloudService.getWorkspace("project", "workspace"))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspaceDetails().bucketName("bucket")));
    Blob mockBlob1 = mock(Blob.class);
    Blob mockBlob2 = mock(Blob.class);
    Blob mockBlob3 = mock(Blob.class);
    when(mockBlob1.getName())
        .thenReturn(NotebooksService.withNotebookExtension("notebooks/mockFile"));
    when(mockBlob2.getName()).thenReturn("notebooks/mockFile.text");
    when(mockBlob3.getName())
        .thenReturn(NotebooksService.withNotebookExtension("notebooks/two words"));
    when(mockCloudStorageClient.getBlobPageForPrefix("bucket", "notebooks"))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2, mockBlob3));

    // Will return 1 entry as only python files in notebook folder are return
    List<String> gotNames =
        notebooksController.getNoteBookList("project", "workspace").getBody().stream()
            .map(FileDetail::getName)
            .collect(Collectors.toList());
    assertThat(gotNames)
        .isEqualTo(
            ImmutableList.of(
                NotebooksService.withNotebookExtension("mockFile"),
                NotebooksService.withNotebookExtension("two words")));
  }

  @Test
  public void testNotebookFileListOmitsExtraDirectories() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    Blob mockBlob1 = mock(Blob.class);
    Blob mockBlob2 = mock(Blob.class);
    when(mockBlob1.getName())
        .thenReturn(NotebooksService.withNotebookExtension("notebooks/extra/nope"));
    when(mockBlob2.getName()).thenReturn(NotebooksService.withNotebookExtension("notebooks/foo"));
    when(mockCloudStorageClient.getBlobPageForPrefix(
            TestMockFactory.WORKSPACE_BUCKET_NAME, "notebooks"))
        .thenReturn(ImmutableList.of(mockBlob1, mockBlob2));

    List<FileDetail> body =
        notebooksController
            .getNoteBookList(workspace.getWorkspaceNamespace(), workspace.getName())
            .getBody();

    List<String> gotNames = body.stream().map(FileDetail::getName).collect(Collectors.toList());
    assertThat(gotNames).isEqualTo(ImmutableList.of(NotebooksService.withNotebookExtension("foo")));
  }

  @Test
  public void testNotebookFileListNotFound() {
    when(mockFireCloudService.getWorkspace("mockProject", "mockWorkspace"))
        .thenThrow(new org.pmiops.workbench.exceptions.NotFoundException());
    assertThrows(
        org.pmiops.workbench.exceptions.NotFoundException.class,
        () -> notebooksController.getNoteBookList("mockProject", "mockWorkspace"));
  }

  @Test
  public void testRenameNotebookInWorkspace() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    String nb1 = NotebooksService.withNotebookExtension("notebooks/nb1");
    String newName = NotebooksService.withNotebookExtension("nb2");
    String newPath = NotebooksService.withNotebookExtension("notebooks/nb2");
    String fullPath = BUCKET_URI + newPath;
    String origFullPath = BUCKET_URI + nb1;
    NotebookRename rename = new NotebookRename();
    rename.setName(NotebooksService.withNotebookExtension("nb1"));
    rename.setNewName(newName);
    notebooksController.renameNotebook(
        workspace.getWorkspaceNamespace(), workspace.getName(), rename);
    verify(mockCloudStorageClient)
        .copyBlob(
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1),
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, newPath));
    verify(mockCloudStorageClient)
        .deleteBlob(BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1));
    verify(mockUserRecentResourceService)
        .updateNotebookEntry(workspace.getWorkspaceId(), currentUser.getUserId(), fullPath);
    verify(mockUserRecentResourceService)
        .deleteNotebookEntry(workspace.getWorkspaceId(), currentUser.getUserId(), origFullPath);
  }

  @Test
  public void testRenameNotebookWoExtension() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    String nb1 = NotebooksService.withNotebookExtension("notebooks/nb1");
    String newName = "nb2";
    String newPath = NotebooksService.withNotebookExtension("notebooks/nb2");
    String fullPath = BUCKET_URI + newPath;
    String origFullPath = BUCKET_URI + nb1;
    NotebookRename rename = new NotebookRename();
    rename.setName(NotebooksService.withNotebookExtension("nb1"));
    rename.setNewName(newName);
    notebooksController.renameNotebook(
        workspace.getWorkspaceNamespace(), workspace.getName(), rename);
    verify(mockCloudStorageClient)
        .copyBlob(
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1),
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, newPath));
    verify(mockCloudStorageClient)
        .deleteBlob(BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1));
    verify(mockUserRecentResourceService)
        .updateNotebookEntry(workspace.getWorkspaceId(), currentUser.getUserId(), fullPath);
    verify(mockUserRecentResourceService)
        .deleteNotebookEntry(workspace.getWorkspaceId(), currentUser.getUserId(), origFullPath);
  }

  @Test
  public void copyNotebook() {
    DbWorkspace fromWorkspace = createWorkspace();
    String fromNotebookName = "origin";

    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = "new";
    String expectedNotebookName = newNotebookName + NotebooksService.NOTEBOOK_EXTENSION;

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.OWNER);

    notebooksController.copyNotebook(
        fromWorkspace.getWorkspaceNamespace(),
        fromWorkspace.getName(),
        fromNotebookName,
        new CopyRequest()
            .toWorkspaceName(toWorkspace.getName())
            .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
            .newName(newNotebookName));

    verify(mockCloudStorageClient)
        .copyBlob(
            BlobId.of(
                TestMockFactory.WORKSPACE_BUCKET_NAME,
                "notebooks/" + NotebooksService.withNotebookExtension(fromNotebookName)),
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, "notebooks/" + expectedNotebookName));

    verify(mockUserRecentResourceService)
        .updateNotebookEntry(
            toWorkspace.getWorkspaceId(),
            currentUser.getUserId(),
            BUCKET_URI + "notebooks/" + expectedNotebookName);
  }

  @Test
  public void copyNotebook_onlyAppendsSuffixIfNeeded() {
    DbWorkspace fromWorkspace = createWorkspace();
    String fromNotebookName = "origin";

    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = NotebooksService.withNotebookExtension("new");

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.OWNER);

    notebooksController.copyNotebook(
        fromWorkspace.getWorkspaceNamespace(),
        fromWorkspace.getName(),
        fromNotebookName,
        new CopyRequest()
            .toWorkspaceName(toWorkspace.getName())
            .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
            .newName(newNotebookName));

    verify(mockCloudStorageClient)
        .copyBlob(
            BlobId.of(
                TestMockFactory.WORKSPACE_BUCKET_NAME,
                "notebooks/" + NotebooksService.withNotebookExtension(fromNotebookName)),
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, "notebooks/" + newNotebookName));
  }

  @Test
  public void copyNotebook_onlyHasReadPermissionsToDestination() {
    DbWorkspace fromWorkspace = createWorkspace();
    String fromNotebookName = "origin";
    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = "new";

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.READER);

    assertThrows(
        ForbiddenException.class,
        () ->
            notebooksController.copyNotebook(
                fromWorkspace.getWorkspaceNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                new CopyRequest()
                    .toWorkspaceName(toWorkspace.getName())
                    .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
                    .newName(newNotebookName)));
  }

  @Test
  public void copyNotebook_noAccessOnSource() {
    DbWorkspace fromWorkspace = createWorkspace("fromWorkspaceNs", "fromworkspace");
    String fromNotebookName = "origin";
    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = "new";

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.NO_ACCESS);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.WRITER);

    assertThrows(
        ForbiddenException.class,
        () ->
            notebooksController.copyNotebook(
                fromWorkspace.getWorkspaceNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                new CopyRequest()
                    .toWorkspaceName(toWorkspace.getName())
                    .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
                    .newName(newNotebookName)));
  }

  @Test
  public void copyNotebook_alreadyExists() {
    DbWorkspace fromWorkspace = createWorkspace();
    String fromNotebookName = "origin";
    DbWorkspace toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace");
    String newNotebookName = NotebooksService.withNotebookExtension("new");

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.OWNER);
    CopyRequest copyNotebookRequest =
        new CopyRequest()
            .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
            .toWorkspaceName(toWorkspace.getName())
            .newName(newNotebookName);
    BlobId newBlobId =
        BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, "notebooks/" + newNotebookName);
    doReturn(Collections.singleton(newBlobId))
        .when(mockCloudStorageClient)
        .getExistingBlobIdsIn(Collections.singletonList(newBlobId));

    assertThrows(
        ConflictException.class,
        () ->
            notebooksController.copyNotebook(
                fromWorkspace.getWorkspaceNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest));
  }

  @Test
  public void testCopyNotebook_notAllowedBetweenTiers() {
    final DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    DbCdrVersion controlledCdr =
        TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao, 2);
    controlledCdr.setName("2");
    controlledCdr.setAccessTier(controlledTier);
    controlledCdr = cdrVersionDao.save(controlledCdr);

    DbWorkspace fromWorkspace = createWorkspace();
    fromWorkspace.setCdrVersion(controlledCdr);
    String fromNotebookName = "origin";

    DbWorkspace toWorkspace =
        workspaceDao.save(
            newWorkspace()
                .setWorkspaceNamespace("to-ns")
                .setName("to-name")
                .setFirecloudName("to-name")
                .setCdrVersion(cdrVersion));
    String newNotebookName = NotebooksService.withNotebookExtension("new");

    stubGetWorkspace(fromWorkspace, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(toWorkspace, WorkspaceAccessLevel.OWNER);

    assertThrows(
        BadRequestException.class,
        () ->
            notebooksController.copyNotebook(
                fromWorkspace.getWorkspaceNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                new CopyRequest()
                    .toWorkspaceName(toWorkspace.getName())
                    .toWorkspaceNamespace(toWorkspace.getWorkspaceNamespace())
                    .newName(newNotebookName)));
  }

  @Test
  public void testCloneNotebook() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    String nb1 = NotebooksService.withNotebookExtension("notebooks/nb1");
    String newPath = NotebooksService.withNotebookExtension("notebooks/Duplicate of nb1");
    String fullPath = BUCKET_URI + newPath;
    notebooksController.cloneNotebook(
        workspace.getWorkspaceNamespace(),
        workspace.getName(),
        NotebooksService.withNotebookExtension("nb1"));
    verify(mockCloudStorageClient)
        .copyBlob(
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1),
            BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, newPath));
    verify(mockUserRecentResourceService)
        .updateNotebookEntry(eq(workspace.getWorkspaceId()), anyLong(), eq(fullPath));
  }

  @Test
  public void testDeleteNotebook() {
    DbWorkspace workspace = createWorkspace();
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    String nb1 = NotebooksService.withNotebookExtension("notebooks/nb1");
    String fullPath = BUCKET_URI + nb1;
    notebooksController.deleteNotebook(
        workspace.getWorkspaceNamespace(),
        workspace.getName(),
        NotebooksService.withNotebookExtension("nb1"));
    verify(mockCloudStorageClient)
        .deleteBlob(BlobId.of(TestMockFactory.WORKSPACE_BUCKET_NAME, nb1));
    verify(mockUserRecentResourceService)
        .deleteNotebookEntry(eq(workspace.getWorkspaceId()), anyLong(), eq(fullPath));
  }

  @Test
  public void notebookLockingEmailHashTest() {
    final String[][] knownTestData = {
      {
        "fc-bucket-id-1",
        "user@aou",
        "dc5acd54f734a2e2350f2adcb0a25a4d1978b45013b76d6bc0a2d37d035292fe"
      },
      {
        "fc-bucket-id-1",
        "another-user@aou",
        "bc90f9f740702e5e0408f2ea13fed9457a7ee9c01117820f5c541067064468c3"
      },
      {
        "fc-bucket-id-2",
        "user@aou",
        "a759e5aef091fd22bbf40bf8ee7cfde4988c668541c18633bd79ab84b274d622"
      },
      // catches an edge case where the hash has a leading 0
      {
        "fc-5ac6bde3-f225-44ca-ad4d-92eed68df7db",
        "brubenst2@fake-research-aou.org",
        "060c0b2ef2385804b7b69a4b4477dd9661be35db270c940525c2282d081aef56"
      }
    };

    for (final String[] test : knownTestData) {
      final String bucket = test[0];
      final String email = test[1];
      final String hash = test[2];

      assertThat(notebooksController.notebookLockingEmailHash(bucket, email)).isEqualTo(hash);
    }
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

  @Test
  public void cloneNotebook_validateActiveBilling() {
    DbWorkspace workspace =
        workspaceDao.save(newWorkspace().setBillingStatus(BillingStatus.INACTIVE));
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    assertThrows(
        ForbiddenException.class,
        () ->
            notebooksController.cloneNotebook(
                workspace.getWorkspaceNamespace(), workspace.getName(), "notebook"));
  }

  @Test
  public void renameNotebook_validateActiveBilling() {
    DbWorkspace workspace =
        workspaceDao.save(newWorkspace().setBillingStatus(BillingStatus.INACTIVE));
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    NotebookRename request = new NotebookRename().name("a").newName("b");

    assertThrows(
        ForbiddenException.class,
        () ->
            notebooksController.renameNotebook(
                workspace.getWorkspaceNamespace(), workspace.getName(), request));
  }

  @Test
  public void copyNotebook_validateActiveBilling() {
    DbWorkspace workspace =
        workspaceDao.save(newWorkspace().setBillingStatus(BillingStatus.INACTIVE));
    stubGetWorkspace(workspace, WorkspaceAccessLevel.OWNER);

    CopyRequest copyNotebookRequest =
        new CopyRequest()
            .toWorkspaceName(workspace.getName())
            .toWorkspaceNamespace(workspace.getWorkspaceNamespace())
            .newName("x");

    assertThrows(
        ForbiddenException.class,
        () ->
            notebooksController.copyNotebook(
                workspace.getWorkspaceNamespace(), workspace.getName(), "z", copyNotebookRequest));
  }

  private void assertNotebookLockingMetadata(
      Map<String, String> gcsMetadata,
      NotebookLockingMetadataResponse expectedResponse,
      FirecloudWorkspaceACL acl) {

    final String testWorkspaceNamespace = "test-ns";
    final String testWorkspaceName = "test-ws";
    final String testNotebook = NotebooksService.withNotebookExtension("test-notebook");

    FirecloudWorkspaceDetails fcWorkspace =
        testMockFactory.createFirecloudWorkspace(
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
        .setFirecloudName("name")
        .setWorkspaceNamespace("namespace")
        .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE)
        .setBillingMigrationStatusEnum(BillingMigrationStatus.NEW)
        .setCdrVersion(cdrVersion)
        .setGoogleProject("proj");
  }

  private DbWorkspace createWorkspace() {
    return createWorkspace(newWorkspace());
  }

  private DbWorkspace createWorkspace(String namespace, String name) {
    return createWorkspace(
        newWorkspace().setWorkspaceNamespace(namespace).setName(name).setFirecloudName(name));
  }

  private DbWorkspace createWorkspace(DbWorkspace workspace) {
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
    when(mockFireCloudService.getWorkspace(workspace.getWorkspaceNamespace(), workspace.getName()))
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
