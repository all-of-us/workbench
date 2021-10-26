package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;

public interface NotebooksService {

  String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  String NOTEBOOK_EXTENSION = ".ipynb";

  static String withNotebookExtension(String notebookName) {
    return notebookName.endsWith(NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(NOTEBOOK_EXTENSION);
  }

  List<FileDetail> getNotebooks(String workspaceNamespace, String firecloudName);

  /**
   * Retrieve all notebooks in the given cloud storage bucket. This method is authenticated as the
   * app engine service account, so authorization must be performed before calling this and the
   * input value should be trusted.
   */
  List<FileDetail> getNotebooksAsService(String bucketName);

  boolean isNotebookBlob(Blob blob);

  FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      String fromNotebookName,
      String toWorkspaceNamespace,
      String toWorkspaceFirecloudName,
      String newNotebookName);

  FileDetail cloneNotebook(String workspaceNamespace, String firecloudName, String notebookName);

  void deleteNotebook(String workspaceNamespace, String firecloudName, String notebookName);

  FileDetail renameNotebook(
      String workspaceNamespace, String firecloudName, String notebookName, String newName);

  JSONObject getNotebookContents(String bucketName, String notebookName);

  KernelTypeEnum getNotebookKernel(JSONObject notebookFile);

  KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String firecloudName, String notebookName);

  void saveNotebook(String bucketName, String notebookName, JSONObject notebookContents);

  String convertNotebookToHtml(byte[] notebook);

  String getReadOnlyHtml(String workspaceNamespace, String firecloudName, String notebookName);

  String adminGetReadOnlyHtml(String workspaceNamespace, String firecloudName, String notebookName);
}
