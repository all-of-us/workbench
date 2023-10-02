package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.springframework.stereotype.Service;

@Service("multicloudNotebookService")
public class MulticloudNotebookService implements NotebooksService {

  private final NotebookServiceFactory notebooksServiceFactory;

  // FIXME We shouldn't need this in the future
  private final NotebooksService gcpNotebookService;

  private final WorkspaceDao workspaceDao;

  public MulticloudNotebookService(
      NotebookServiceFactory notebooksServiceFactory,
      NotebooksService gcpNotebookService,
      WorkspaceDao workspaceDao) {
    this.notebooksServiceFactory = notebooksServiceFactory;
    this.gcpNotebookService = gcpNotebookService;
    this.workspaceDao = workspaceDao;
  }

  @Override
  public List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName) {
    NotebooksService notebooksService = getNotebooksService(workspaceNamespace, workspaceName);
    return notebooksService.getNotebooks(workspaceNamespace, workspaceName);
  }

  @Override
  public List<FileDetail> getNotebooksAsService(
      String bucketName, String workspaceNamespace, String workspaceName) {
    NotebooksService notebooksService = getNotebooksService(workspaceNamespace, workspaceName);
    return notebooksService.getNotebooksAsService(bucketName, workspaceNamespace, workspaceName);
  }

  @Override
  public boolean isManagedNotebookBlob(Blob blob) {
    // FIXME need to be fixed by abstracting the Blob object and make it work for other clouds.
    return gcpNotebookService.isManagedNotebookBlob(blob);
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

  private NotebooksService getNotebooksService(String workspaceNamespace, String workspaceName) {
    DbWorkspace dbWorkspace = workspaceDao.get(workspaceNamespace, workspaceName);
    NotebooksService notebooksService = notebooksServiceFactory.getNotebookService(dbWorkspace);
    return notebooksService;
  }
}
