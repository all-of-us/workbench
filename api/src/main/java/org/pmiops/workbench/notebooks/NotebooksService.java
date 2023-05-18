package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;

public interface NotebooksService {

  List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName);

  /**
   * Retrieve all notebooks in the given cloud storage bucket. This method is authenticated as the
   * app engine service account, so authorization must be performed before calling this and the
   * input value should be trusted.
   */
  List<FileDetail> getNotebooksAsService(
      String bucketName, String workspaceNamespace, String workspaceName);

  boolean isNotebookBlob(Blob blob);

  FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceName,
      String fromNotebookNameWithExtension,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newNotebookNameWithExtension);

  FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceName, String notebookNameWithExtension);

  void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  FileDetail renameNotebook(
      String workspaceNamespace,
      String workspaceName,
      String originalNameWithExtension,
      String newNameWithExtension);

  JSONObject getNotebookContents(String bucketName, String notebookName);

  KernelTypeEnum getNotebookKernel(JSONObject notebookFile);

  KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String workspaceName, String notebookName);

  void saveNotebook(
      String bucketName, String notebookNameWithFileExtension, JSONObject notebookContents);

  public String convertJupyterNotebookToHtml(byte[] notebook);

  String getReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);

  String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceName, String notebookNameWithFileExtension);
}
