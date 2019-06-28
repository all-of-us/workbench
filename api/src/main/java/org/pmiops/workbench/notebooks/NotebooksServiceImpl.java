package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.GoogleCloudLocators;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private final Clock clock;
  private final CloudStorageService cloudStorageService;
  private final FireCloudService fireCloudService;
  private final Provider<User> userProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final WorkspaceService workspaceService;

  @Autowired
  public NotebooksServiceImpl(
      Clock clock,
      CloudStorageService cloudStorageService,
      FireCloudService fireCloudService,
      Provider<User> userProvider,
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
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

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
  public FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceName,
      String fromNotebookName,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newNotebookName) {
    newNotebookName = appendSuffixIfNeeded(newNotebookName);
    GoogleCloudLocators fromNotebookLocators =
        getNotebookLocators(fromWorkspaceNamespace, fromWorkspaceName, fromNotebookName);
    GoogleCloudLocators newNotebookLocators =
        getNotebookLocators(toWorkspaceNamespace, toWorkspaceName, newNotebookName);

    workspaceService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceName, WorkspaceAccessLevel.READER);
    workspaceService.enforceWorkspaceAccessLevel(
        toWorkspaceNamespace, toWorkspaceName, WorkspaceAccessLevel.WRITER);
    if (!cloudStorageService
        .blobsExist(Collections.singletonList(newNotebookLocators.blobId))
        .isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
    cloudStorageService.copyBlob(fromNotebookLocators.blobId, newNotebookLocators.blobId);

    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(newNotebookName);
    fileDetail.setPath(newNotebookLocators.fullPath);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    fileDetail.setLastModifiedTime(now.getTime());
    userRecentResourceService.updateNotebookEntry(
        workspaceService.getRequired(toWorkspaceNamespace, toWorkspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        newNotebookLocators.fullPath,
        now);

    return fileDetail;
  }

  @Override
  public FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceName, String fromNotebookName) {
    String newName = "Duplicate of " + fromNotebookName;
    return copyNotebook(
        workspaceNamespace,
        workspaceName,
        fromNotebookName,
        workspaceNamespace,
        workspaceName,
        newName);
  }

  @Override
  public void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName) {
    GoogleCloudLocators notebookLocators =
        getNotebookLocators(workspaceNamespace, workspaceName, notebookName);
    cloudStorageService.deleteBlob(notebookLocators.blobId);
    userRecentResourceService.deleteNotebookEntry(
        workspaceService.getRequired(workspaceNamespace, workspaceName).getWorkspaceId(),
        userProvider.get().getUserId(),
        notebookLocators.fullPath);
  }

  @Override
  public FileDetail renameNotebook(
      String workspaceNamespace, String workspaceName, String originalName, String newName) {
    FileDetail fileDetail =
        copyNotebook(
            workspaceNamespace,
            workspaceName,
            originalName,
            workspaceNamespace,
            workspaceName,
            appendSuffixIfNeeded(newName));
    deleteNotebook(workspaceNamespace, workspaceName, originalName);

    return fileDetail;
  }

  @Override
  public JSONObject getNotebookContents(String bucketName, String notebookName) {
    try {
      return cloudStorageService.getFileAsJson(
          bucketName, "notebooks/".concat(withNotebookExtension(notebookName)));
    } catch (IOException e) {
      throw new ServerErrorException(
          "Failed to get notebook " + notebookName + " from bucket " + bucketName);
    }
  }

  private String withNotebookExtension(String notebookName) {
    return notebookName.endsWith(".ipynb") ? notebookName : notebookName.concat(".ipynb");
  }

  @Override
  public void saveNotebook(String bucketName, String notebookName, JSONObject notebookContents) {
    cloudStorageService.writeFile(
        bucketName,
        "notebooks/" + notebookName + ".ipynb",
        notebookContents.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String getReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookName) {
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    // We need to send a byte array so the ApiClient attaches the body as is instead
    // of serializing it through Gson which it will do for Strings.
    // The default Gson serializer does not work since it strips out some null fields
    // which are needed for nbconvert
    return fireCloudService.staticNotebooksConvert(
        getNotebookContents(bucketName, notebookName).toString().getBytes());
  }

  private String appendSuffixIfNeeded(String filename) {
    if (!filename.matches("^.+\\.ipynb")) {
      return filename + ".ipynb";
    }

    return filename;
  }

  private GoogleCloudLocators getNotebookLocators(
      String workspaceNamespace, String workspaceName, String notebookName) {
    String bucket =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();
    String blobPath = NOTEBOOKS_WORKSPACE_DIRECTORY + "/" + notebookName;
    String pathStart = "gs://" + bucket + "/";
    String fullPath = pathStart + blobPath;
    BlobId blobId = BlobId.of(bucket, blobPath);
    return new GoogleCloudLocators(blobId, fullPath);
  }
}
