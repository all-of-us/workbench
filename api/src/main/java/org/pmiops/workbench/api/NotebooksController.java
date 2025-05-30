package org.pmiops.workbench.api;

import static org.pmiops.workbench.lab.notebooks.NotebookUtils.appendFileExtensionIfMissing;

import jakarta.inject.Provider;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.lab.notebooks.NotebookUtils;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeResponse;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebookLockingUtils;
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
      String workspaceNamespace, String workspaceTerraName) {
    return ResponseEntity.ok(notebooksService.getNotebooks(workspaceNamespace, workspaceTerraName));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookName) {
    notebooksService.deleteNotebook(workspaceNamespace, workspaceTerraName, notebookName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceTerraName,
      String fromNotebookNameWithExtension,
      CopyRequest copyRequest) {
    return ResponseEntity.ok(
        copyNotebookImpl(
            fromWorkspaceNamespace,
            fromWorkspaceTerraName,
            fromNotebookNameWithExtension,
            copyRequest));
  }

  private FileDetail copyNotebookImpl(
      String fromWorkspaceNamespace,
      String fromWorkspaceTerraName,
      String fromNotebookNameWithExtension,
      CopyRequest copyRequest) {
    FileDetail fileDetail;
    try {
      // Checks the new name extension to match the original from file type, add extension if
      // needed.
      // TODO(yonghao): Remove withNotebookExtension after UI start setting extension.
      String newNameWithExtension =
          appendFileExtensionIfMissing(fromNotebookNameWithExtension, copyRequest.getNewName());

      fileDetail =
          notebooksService.copyNotebook(
              fromWorkspaceNamespace,
              fromWorkspaceTerraName,
              fromNotebookNameWithExtension,
              copyRequest.getToWorkspaceNamespace(),
              copyRequest.getToWorkspaceTerraName(),
              newNameWithExtension);
    } catch (BlobAlreadyExistsException e) {
      throw new ConflictException("File already exists at copy destination");
    }
    return fileDetail;
  }

  @Override
  public ResponseEntity<FileDetail> cloneNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithExtension) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.cloneNotebook(
              workspaceNamespace, workspaceTerraName, notebookNameWithExtension);
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<ReadOnlyNotebookResponse> readOnlyNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithFileExtension) {
    ReadOnlyNotebookResponse response =
        new ReadOnlyNotebookResponse()
            .html(
                notebooksService.getReadOnlyHtml(
                    workspaceNamespace, workspaceTerraName, notebookNameWithFileExtension));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<FileDetail> renameNotebook(
      String workspaceNamespace, String workspaceTerraName, NotebookRename rename) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.renameNotebook(
              workspaceNamespace, workspaceTerraName, rename.getName(), rename.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<KernelTypeResponse> getNotebookKernel(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithFileExtension) {
    if (!NotebookUtils.isJupyterNotebook(notebookNameWithFileExtension)) {
      throw new BadRequestException(
          String.format("%s is not a Jupyter notebook file", notebookNameWithFileExtension));
    }

    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceTerraName, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new KernelTypeResponse()
            .kernelType(
                notebooksService.getNotebookKernel(
                    workspaceNamespace, workspaceTerraName, notebookNameWithFileExtension)));
  }

  @Override
  public ResponseEntity<NotebookLockingMetadataResponse> getNotebookLockingMetadata(
      String workspaceNamespace, String workspaceTerraName, String notebookName) {

    // Retrieving the workspace is done first, which acts as an access check.
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceTerraName)
            .getWorkspace()
            .getBucketName();

    // response may be empty - fill in what we can
    NotebookLockingMetadataResponse response = new NotebookLockingMetadataResponse();

    // throws NotFoundException if the notebook is not in GCS
    // returns null if found but no user-metadata
    Map<String, String> metadata =
        cloudStorageClient.getMetadata(bucketName, NotebookUtils.withNotebookPath(notebookName));

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
                .getFirecloudWorkspaceAcl(workspaceNamespace, workspaceTerraName)
                .keySet();

        response.lastLockedBy(
            NotebookLockingUtils.findHashedUser(bucketName, workspaceUsers, lastLockedByHash));
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
}
