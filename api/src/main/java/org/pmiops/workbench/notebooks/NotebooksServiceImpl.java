package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.BlobId;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import javax.inject.Provider;
import org.pmiops.workbench.auth.UserProvider;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotebooksServiceImpl implements NotebooksService {

  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";

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
  public FileDetail copyNotebook(String fromWorkspace, String fromWorkspaceName, String fromNotebookName,
      String toWorkspace, String toWorkspaceName, String toNotebookName) {
    NotebookCloudConfig fromNotebookConfig = getNotebookCloudConfig(fromWorkspace, fromWorkspaceName, fromNotebookName);
    NotebookCloudConfig newNotebookConfig = getNotebookCloudConfig(toWorkspace, toWorkspaceName, toNotebookName);

    if (!cloudStorageService.blobsExist(Collections.singletonList(newNotebookConfig.blobId)).isEmpty()) {
      throw new BlobAlreadyExistsException();
    }
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
}
