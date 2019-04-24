package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.pmiops.workbench.auth.UserProvider;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  private static final Pattern NOTEBOOK_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");

  private final Clock clock;
  private final CloudStorageService cloudStorageService;
  private final FireCloudService fireCloudService;
  private final UserProvider userProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final WorkspaceService workspaceService;

  @Autowired
  public NotebooksServiceImpl(Clock clock,
      CloudStorageService cloudStorageService,
      FireCloudService fireCloudService,
      UserProvider userProvider,
      UserRecentResourceService userRecentResourceService,
      WorkspaceService workspaceService) {
    this.clock = clock;
    this.cloudStorageService = cloudStorageService;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workspaceService = workspaceService;
  }

  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName) {
    List<FileDetail> notebooks = getFilesFromNotebooks(fireCloudService
          .getWorkspace(workspaceNamespace, workspaceName)
          .getWorkspace().getBucketName());
    return notebooks;
  }

  @Override
  public FileDetail copyNotebook(String fromWorkspace, String fromWorkspaceName, String fromNotebookName,
      String toWorkspace, String toWorkspaceName, String toNotebookName) {
    NotebookCloudConfig fromNotebookConfig = getNotebookCloudConfig(fromWorkspace, fromWorkspaceName, fromNotebookName);
    NotebookCloudConfig newNotebookConfig = getNotebookCloudConfig(toWorkspace, toWorkspaceName, toNotebookName);

    if (!cloudStorageService.blobsExist(Collections.singletonList(newNotebookConfig.blobId)).isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    workspaceService.enforceWorkspaceAccessLevel(toWorkspace, toWorkspaceName, WorkspaceAccessLevel.WRITER);

    cloudStorageService.copyBlob(fromNotebookConfig.blobId, newNotebookConfig.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(toNotebookName);
    fileDetail.setPath(newNotebookConfig.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService
        .updateNotebookEntry(
            workspaceService.getRequired(toWorkspace, toWorkspaceName).getWorkspaceId(),
            userProvider.get().getUserId(),
            newNotebookConfig.fullPath,
            now);

    return fileDetail;
  }

  @Override
  public FileDetail cloneNotebook(String workspaceNamespace, String workspaceName, String fromNotebookName) {
    String newName = fromNotebookName.replaceAll("\\.ipynb", " ") + "Clone.ipynb";
    return copyNotebook(workspaceNamespace, workspaceName, fromNotebookName,
        workspaceNamespace, workspaceName, newName);
  }

  @Override
  public void deleteNotebook(String workspaceNamespace, String workspaceName,
      String notebookName) {
    NotebookCloudConfig config = getNotebookCloudConfig(workspaceNamespace, workspaceName, notebookName);
    cloudStorageService.deleteBlob(config.blobId);
    userRecentResourceService.deleteNotebookEntry(
        workspaceService.getRequired(workspaceNamespace, workspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        config.fullPath
    );
  }

  @Override
  public FileDetail renameNotebook(String workspaceNamespace, String workspaceName,
      String originalName, String newName) {
    if (!newName.matches("^.+\\.ipynb")) {
      newName = newName + ".ipynb";
    }
    FileDetail fileDetail = copyNotebook(workspaceNamespace, workspaceName, originalName,
        workspaceNamespace, workspaceName, newName);
    deleteNotebook(workspaceNamespace, workspaceName, originalName);

    return fileDetail;
  }

  private class NotebookCloudConfig {
    public final BlobId blobId;
    public final String fullPath;

    public NotebookCloudConfig(BlobId blobId, String fullPath) {
      this.blobId = blobId;
      this.fullPath = fullPath;
    }
  }

  private NotebookCloudConfig getNotebookCloudConfig(String workspaceNamespace, String workspaceName, String notebookName) {
    String bucket = fireCloudService.getWorkspace(workspaceNamespace, workspaceName)
        .getWorkspace()
        .getBucketName();
    String blobPath = NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new NotebookCloudConfig(blobId, fullPath);
  }

  /**
   * Returns List of python fileDetails from notebooks folder
   *
   * @return list of FileDetail
   */
  private List<FileDetail> getFilesFromNotebooks(String bucketName) {
    return cloudStorageService.getBlobList(bucketName, NOTEBOOKS_WORKSPACE_DIRECTORY).stream()
        .filter(blob -> NOTEBOOK_PATTERN.matcher(blob.getName()).matches())
        .map(blob -> blobToFileDetail(blob, bucketName))
        .collect(Collectors.toList());
  }

  private FileDetail blobToFileDetail(Blob blob, String bucketName) {
    String[] parts = blob.getName().split("/");
    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(parts[parts.length - 1]);
    fileDetail.setPath("gs://" + bucketName + "/" + blob.getName());
    fileDetail.setLastModifiedTime(blob.getUpdateTime());
    return fileDetail;
  }
}
