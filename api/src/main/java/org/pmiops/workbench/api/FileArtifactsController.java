package org.pmiops.workbench.api;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.BlobAlreadyExistsException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeResponse;
import org.pmiops.workbench.model.FileArtifactLockingMetadataResponse;
import org.pmiops.workbench.model.FileArtifactsRename;
import org.pmiops.workbench.model.ReadOnlyFileArtifactResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.fileArtifacts.FileArtifactLockingUtils;
import org.pmiops.workbench.fileArtifacts.FileArtifactsService;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileArtifactsController implements FileArtifactsApiDelegate {
  private static final Logger log = Logger.getLogger(FileArtifactsController.class.getName());

  @Autowired private Clock clock;
  @Autowired private CloudStorageClient cloudStorageClient;
  @Autowired private FileArtifactsService fileArtifactsService;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private WorkspaceAuthService workspaceAuthService;
  @Autowired private Provider<DbUser> userProvider;

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(fileArtifactsService.getFileArtifacts(workspaceNamespace, workspaceId));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteFileArtifact(
      String workspace, String workspaceName, String fileArtifactName) {
    fileArtifactsService.deleteFileArtifact(workspace, workspaceName, fileArtifactName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyFileArtifact(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromFileArtifactName,
      CopyRequest copyRequest) {
    return ResponseEntity.ok(
        copyFileArtifactImpl(fromWorkspaceNamespace, fromWorkspaceId, fromFileArtifactName, copyRequest));
  }

  private FileDetail copyFileArtifactImpl(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromFileArtifactName,
      CopyRequest copyRequest) {
    FileDetail fileDetail;
    try {
      fileDetail =
          fileArtifactsService.copyFileArtifact(
              fromWorkspaceNamespace,
              fromWorkspaceId,
              FileArtifactsService.withFileArtifactExtension(fromFileArtifactName),
              copyRequest.getToWorkspaceNamespace(),
              copyRequest.getToWorkspaceName(),
              FileArtifactsService.withFileArtifactExtension(copyRequest.getNewName()));
    } catch (BlobAlreadyExistsException e) {
      throw new ConflictException("File already exists at copy destination");
    }
    return fileDetail;
  }

  @Override
  public ResponseEntity<FileDetail> cloneFileArtifact(
      String workspace, String workspaceName, String fileArtifactName) {
    FileDetail fileDetail;
    try {
      fileDetail = fileArtifactsService.cloneFileArtifact(workspace, workspaceName, fileArtifactName);
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<ReadOnlyFileArtifactResponse> readOnlyFileArtifact(
      String workspaceNamespace, String workspaceName, String fileArtifactName) {
    ReadOnlyFileArtifactResponse response =
        new ReadOnlyFileArtifactResponse()
            .html(
                fileArtifactsService.getReadOnlyHtml(workspaceNamespace, workspaceName, fileArtifactName));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<FileDetail> renameFileArtifact(
      String workspace, String workspaceName, FileArtifactsRename rename) {
    FileDetail fileDetail;
    try {
      fileDetail =
          fileArtifactsService.renameFileArtifact(
              workspace, workspaceName, rename.getName(), rename.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<KernelTypeResponse> getFileArtifactKernel(
      String workspace, String workspaceName, String fileArtifactName) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspace, workspaceName, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new KernelTypeResponse()
            .kernelType(
                fileArtifactsService.getFileArtifactKernel(workspace, workspaceName, fileArtifactName)));
  }

  @Override
  public ResponseEntity<FileArtifactLockingMetadataResponse> getFileArtifactLockingMetadata(
      String workspaceNamespace, String workspaceName, String fileArtifactName) {

    // Retrieving the workspace is done first, which acts as an access check.
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    // response may be empty - fill in what we can
    FileArtifactLockingMetadataResponse response = new FileArtifactLockingMetadataResponse();

    // throws NotFoundException if the fileArtifact is not in GCS
    // returns null if found but no user-metadata
    Map<String, String> metadata =
        cloudStorageClient.getMetadata(bucketName, "fileArtifacts/" + fileArtifactName);

    if (metadata != null) {
      String lockExpirationTime = metadata.get("lockExpiresAt");
      if (lockExpirationTime != null) {
        response.lockExpirationTime(Long.valueOf(lockExpirationTime));
      }

      // stored as a SHA-256 hash of bucketName:userEmail
      String lastLockedByHash = metadata.get("lastLockedBy");
      if (lastLockedByHash != null) {

        // the caller should not necessarily know the identities of all fileArtifact users
        // so we check against the set of users of this workspace which are known to the caller

        // NOTE: currently, users of workspace X of any access level can see all other
        // workspace X users. This is not desired.
        // https://precisionmedicineinitiative.atlassian.net/browse/RW-3094

        Set<String> workspaceUsers =
            workspaceAuthService
                .getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName)
                .keySet();

        response.lastLockedBy(
            FileArtifactLockingUtils.findHashedUser(bucketName, workspaceUsers, lastLockedByHash));
      }
    }

    // If a lock is held by another user, log this to establish a rough estimate of how often
    // locked fileArtifacts are encountered. Note that this only covers locks encountered from the
    // Workbench - any Jupyter UI-based lock detection does not touch this code path.
    String currentUsername = userProvider.get().getUsername();
    if (response.getLockExpirationTime() == null
        || response.getLastLockedBy() == null
        || response.getLockExpirationTime() < clock.millis()
        || response.getLastLockedBy().equals(currentUsername)) {
      log.info(String.format("user '%s' observed fileArtifact available for editing", currentUsername));
    } else {
      log.info(
          String.format(
              "user '%s' observed fileArtifact locked by '%s'",
              currentUsername, response.getLastLockedBy()));
    }

    return ResponseEntity.ok(response);
  }
}
