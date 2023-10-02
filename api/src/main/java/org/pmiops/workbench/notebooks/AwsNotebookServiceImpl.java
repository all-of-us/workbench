package org.pmiops.workbench.notebooks;

import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import com.google.cloud.storage.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("awsNotebookService")
public class AwsNotebookServiceImpl implements NotebooksService {

  private final Provider<ResourceApi> resourceApiProvider;

  private final WorkspaceDao workspaceDao;

  @Autowired
  public AwsNotebookServiceImpl(
      Provider<ResourceApi> resourceApiProvider, WorkspaceDao workspaceDao) {
    this.resourceApiProvider = resourceApiProvider;
    this.workspaceDao = workspaceDao;
  }

  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName) {
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceName);
    List<FileDetail> fileDetails = new ArrayList<>();
    try {
      ResourceList resourceList =
          resourceApiProvider
              .get()
              .enumerateResources(
                  UUID.fromString(workspace.getFirecloudUuid()),
                  0,
                  10,
                  ResourceType.AWS_SAGEMAKER_NOTEBOOK,
                  StewardshipType.CONTROLLED);
      resourceList
          .getResources()
          .forEach(
              resource -> {
                ResourceMetadata metadata = resource.getMetadata();
                fileDetails.add(
                    new FileDetail()
                        .name(metadata.getName())
                        .lastModifiedTime(metadata.getLastUpdatedDate().toEpochSecond())
                        .lastModifiedBy(metadata.getLastUpdatedBy()));
              });
    } catch (ApiException e) {
      throw new WorkbenchException(e);
    }
    return fileDetails;
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
