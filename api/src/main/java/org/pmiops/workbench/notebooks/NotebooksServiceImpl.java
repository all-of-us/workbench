package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.pmiops.workbench.auth.UserProvider;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  public static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  public static final Pattern NOTEBOOK_PATTERN =
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
    String bucketName = fireCloudService.getWorkspace(workspaceNamespace, workspaceName)
        .getWorkspace().getBucketName();

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

  @Override
  public FileDetail copyNotebook(String fromWorkspaceNamespace, String fromWorkspaceName, String fromNotebookName,
      String toWorkspaceNamespace, String toWorkspaceName, String newNotebookName) {
    GoogleCloudLocators fromNotebookLocators = getNotebookLocators(fromWorkspaceNamespace, fromWorkspaceName, fromNotebookName);
    GoogleCloudLocators newNotebookLocators = getNotebookLocators(toWorkspaceNamespace, toWorkspaceName, newNotebookName);

    if (!cloudStorageService.blobsExist(Collections.singletonList(newNotebookLocators.blobId)).isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    workspaceService.enforceWorkspaceAccessLevel(toWorkspaceNamespace, toWorkspaceName, WorkspaceAccessLevel.WRITER);

    cloudStorageService.copyBlob(fromNotebookLocators.blobId, newNotebookLocators.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(newNotebookName);
    fileDetail.setPath(newNotebookLocators.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService
        .updateNotebookEntry(
            workspaceService.getRequired(toWorkspaceNamespace, toWorkspaceName).getWorkspaceId(),
            userProvider.get().getUserId(),
            newNotebookLocators.fullPath,
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
    GoogleCloudLocators notebookLocators = getNotebookLocators(workspaceNamespace, workspaceName, notebookName);
    cloudStorageService.deleteBlob(notebookLocators.blobId);
    userRecentResourceService.deleteNotebookEntry(
        workspaceService.getRequired(workspaceNamespace, workspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        notebookLocators.fullPath
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

  private class GoogleCloudLocators {
    public final BlobId blobId;
    public final String fullPath;

    public GoogleCloudLocators(BlobId blobId, String fullPath) {
      this.blobId = blobId;
      this.fullPath = fullPath;
    }
  }

  private GoogleCloudLocators getNotebookLocators(String workspaceNamespace, String workspaceName, String notebookName) {
    String bucket = fireCloudService.getWorkspace(workspaceNamespace, workspaceName)
        .getWorkspace()
        .getBucketName();
    String blobPath = NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new GoogleCloudLocators(blobId, fullPath);
  }

}
