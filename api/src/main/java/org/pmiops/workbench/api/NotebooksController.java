package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeResponse;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.BlobAlreadyExistsException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotebooksController implements NotebooksApiDelegate {
  private static final Logger log = Logger.getLogger(NotebooksController.class.getName());

  @Autowired private Clock clock;
  @Autowired private CloudStorageClient cloudStorageClient;
  @Autowired private NotebooksService notebooksService;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private WorkspaceAuthService workspaceAuthService;
  @Autowired private Provider<DbUser> userProvider;

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(notebooksService.getNotebooks(workspaceNamespace, workspaceId));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteNotebook(
      String workspace, String workspaceName, String notebookName) {
    notebooksService.deleteNotebook(workspace, workspaceName, notebookName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromNotebookName,
      CopyRequest copyRequest) {
    return ResponseEntity.ok(
        copyNotebookImpl(fromWorkspaceNamespace, fromWorkspaceId, fromNotebookName, copyRequest));
  }

  private FileDetail copyNotebookImpl(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromNotebookName,
      CopyRequest copyRequest) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.copyNotebook(
              fromWorkspaceNamespace,
              fromWorkspaceId,
              NotebooksService.withNotebookExtension(fromNotebookName),
              copyRequest.getToWorkspaceNamespace(),
              copyRequest.getToWorkspaceName(),
              NotebooksService.withNotebookExtension(copyRequest.getNewName()));
    } catch (BlobAlreadyExistsException e) {
      throw new ConflictException("File already exists at copy destination");
    }
    return fileDetail;
  }

  @Override
  public ResponseEntity<FileDetail> cloneNotebook(
      String workspace, String workspaceName, String notebookName) {
    FileDetail fileDetail;
    try {
      fileDetail = notebooksService.cloneNotebook(workspace, workspaceName, notebookName);
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<ReadOnlyNotebookResponse> readOnlyNotebook(
      String workspaceNamespace, String workspaceName, String notebookName) {
    ReadOnlyNotebookResponse response =
        new ReadOnlyNotebookResponse()
            .html(
                notebooksService.getReadOnlyHtml(workspaceNamespace, workspaceName, notebookName));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<FileDetail> renameNotebook(
      String workspace, String workspaceName, NotebookRename rename) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.renameNotebook(
              workspace, workspaceName, rename.getName(), rename.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<KernelTypeResponse> getNotebookKernel(
      String workspace, String workspaceName, String notebookName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspace, workspaceName, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new KernelTypeResponse()
            .kernelType(
                notebooksService.getNotebookKernel(workspace, workspaceName, notebookName)));
  }

  @Override
  public ResponseEntity<NotebookLockingMetadataResponse> getNotebookLockingMetadata(
      String workspaceNamespace, String workspaceName, String notebookName) {

    // Retrieving the workspace is done first, which acts as an access check.
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    // response may be empty - fill in what we can
    NotebookLockingMetadataResponse response = new NotebookLockingMetadataResponse();

    // throws NotFoundException if the notebook is not in GCS
    // returns null if found but no user-metadata
    Map<String, String> metadata =
        cloudStorageClient.getMetadata(bucketName, "notebooks/" + notebookName);

    if (metadata != null) {
      String lockExpirationTime = metadata.get("lockExpiresAt");
      if (lockExpirationTime != null) {
        response.lockExpirationTime(Long.valueOf(lockExpirationTime));
      }

      // stored as a SHA-256 hash of bucketName:userEmail
      String lastLockedByHash = metadata.get("lastLockedBy");
      if (lastLockedByHash != null) {

        // the caller should not necessarily know the identities of all notebook users
        // so we check against the set of users of this workspace which are known to the caller

        // NOTE: currently, users of workspace X of any access level can see all other
        // workspace X users. This is not desired.
        // https://precisionmedicineinitiative.atlassian.net/browse/RW-3094

        Set<String> workspaceUsers =
            workspaceAuthService
                .getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName)
                .keySet();

        response.lastLockedBy(findHashedUser(bucketName, workspaceUsers, lastLockedByHash));
      }
    }

    // If a lock is held by another user, log this to establish a rough estimate of how often
    // locked notebooks are encountered. Note that this only covers locks encountered from the
    // Workbench - any Jupyter UI-based lock detection does not touch this code path.
    String currentUsername = userProvider.get().getUsername();
    if (response.getLockExpirationTime() == null
        || response.getLastLockedBy() == null
        || response.getLockExpirationTime() < clock.millis()
        || response.getLastLockedBy().equals(currentUsername)) {
      log.info(String.format("user '%s' observed notebook available for editing", currentUsername));
    } else {
      log.info(
          String.format(
              "user '%s' observed notebook locked by '%s'",
              currentUsername, response.getLastLockedBy()));
    }

    return ResponseEntity.ok(response);
  }

  private String findHashedUser(String bucket, Set<String> workspaceUsers, String hash) {
    return workspaceUsers.stream()
        .filter(email -> notebookLockingEmailHash(bucket, email).equals(hash))
        .findAny()
        .orElse("UNKNOWN");
  }

  @VisibleForTesting
  static String notebookLockingEmailHash(String bucket, String email) {
    String toHash = String.format("%s:%s", bucket, email);
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] hash = sha256.digest(toHash.getBytes(StandardCharsets.UTF_8));
      // convert to printable hex text
      return BaseEncoding.base16().lowerCase().encode(hash);
    } catch (final NoSuchAlgorithmException e) {
      throw new ServerErrorException(e);
    }
  }
}
