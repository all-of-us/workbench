package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;

public interface NotebooksService {

  String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  String NOTEBOOK_EXTENSION = ".ipynb";

  static String withNotebookExtension(String notebookName) {
    return notebookName.endsWith(NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(NOTEBOOK_EXTENSION);
  }

  List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName);

  /**
   * Retrieve all notebooks in the given cloud storage bucket. This method is authenticated as the
   * app engine service account, so authorization must be performed before calling this and the
   * input value should be trusted.
   */
  List<FileDetail> getNotebooksAsService(String bucketName);

  boolean isNotebookBlob(Blob blob);

  FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceName,
      String fromNotebookName,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newNotebookName);

  FileDetail cloneNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  FileDetail renameNotebook(
      String workspaceNamespace, String workspaceName, String notebookName, String newName);

  JSONObject getNotebookContents(String bucketName, String notebookName);

  void saveNotebook(String bucketName, String notebookName, JSONObject notebookContents);

  public String convertNotebookToHtml(byte[] notebook);

  String getReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);

  String adminGetReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);
}
