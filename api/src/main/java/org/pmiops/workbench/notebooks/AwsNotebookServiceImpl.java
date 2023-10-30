package org.pmiops.workbench.notebooks;

import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.ResourceDescription;
import com.google.cloud.storage.Blob;
import java.util.List;
import java.util.UUID;
import org.json.JSONObject;
import org.pmiops.workbench.aws.s3.AwsS3CloudStorageClient;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.wsm.WsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("awsNotebookService")
public class AwsNotebookServiceImpl implements NotebooksService {

  private final WsmClient wsmClient;
  private final WorkspaceDao workspaceDao;

  private final AwsS3CloudStorageClient s3CloudStorageClient;

  @Autowired
  public AwsNotebookServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      AwsS3CloudStorageClient s3CloudStorageClient) {
    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.s3CloudStorageClient = s3CloudStorageClient;
  }

  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName) {

    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceName);
    ResourceDescription awsS3Folder =
        wsmClient.getAwsS3Folder(UUID.fromString(workspace.getFirecloudUuid()));
    try {
      AwsCredential awsS3Credential =
          wsmClient.getAwsS3Credential(
              workspace.getFirecloudUuid(), awsS3Folder.getMetadata().getResourceId());
      return s3CloudStorageClient.getFilesFromS3(
          awsS3Folder.getResourceAttributes().getAwsS3StorageFolder(), awsS3Credential);

    } catch (ApiException e) {
      throw new WorkbenchException(e);
    }
  }

  @Override
  public List<FileDetail> getNotebooksAsService(
      String bucketName, String workspaceNamespace, String workspaceName) {
    return null;
  }

  @Override
  public boolean isManagedNotebookBlob(Blob blob) {
    return false;
  }

  @Override
  public FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceName,
      String fromNotebookNameWithExtension,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newNotebookNameWithExtension) {
    return null;
  }

  @Override
  public FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceName, String notebookNameWithExtension) {
    return null;
  }

  @Override
  public void deleteNotebook(
      String workspaceNamespace, String workspaceName, String notebookName) {}

  @Override
  public FileDetail renameNotebook(
      String workspaceNamespace,
      String workspaceName,
      String originalNameWithExtension,
      String newNameWithExtension) {
    return null;
  }

  @Override
  public JSONObject getNotebookContents(String bucketName, String notebookName) {
    return null;
  }

  @Override
  public KernelTypeEnum getNotebookKernel(JSONObject notebookFile) {
    return null;
  }

  @Override
  public KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String workspaceName, String notebookName) {
    return null;
  }

  @Override
  public void saveNotebook(
      String bucketName, String notebookNameWithFileExtension, JSONObject notebookContents) {}

  @Override
  public String convertJupyterNotebookToHtml(byte[] notebook) {
    return null;
  }

  @Override
  public String getReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookName) {
    return null;
  }

  @Override
  public String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookNameWithFileExtension) {
    return null;
  }
}
