package org.pmiops.workbench.notebooks;

import static org.pmiops.workbench.notebooks.NotebookUtils.JUPYTER_NOTEBOOK_EXTENSION;
import static org.pmiops.workbench.notebooks.NotebookUtils.R_MARKDOWN_NOTEBOOK_EXTENSION;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;

public interface NotebooksService {
  static String withJupyterNotebookExtension(String notebookName) {
    return notebookName.endsWith(JUPYTER_NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(JUPYTER_NOTEBOOK_EXTENSION);
  }

  static String withRMarkdownExtension(String notebookName) {
    return notebookName.endsWith(R_MARKDOWN_NOTEBOOK_EXTENSION)
        ? notebookName
        : notebookName.concat(R_MARKDOWN_NOTEBOOK_EXTENSION);
  }

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
      String fromNotebookName,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newNotebookName);

  FileDetail cloneNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  FileDetail renameNotebook(
      String workspaceNamespace, String workspaceName, String notebookName, String newName);

  JSONObject getNotebookContents(String bucketName, String notebookName);

  KernelTypeEnum getNotebookKernel(JSONObject notebookFile);

  KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String workspaceName, String notebookName);

  void saveNotebook(String bucketName, String notebookName, JSONObject notebookContents);

  public String convertNotebookToHtml(byte[] notebook);

  String getReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);

  String adminGetReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);
}
