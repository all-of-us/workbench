package org.pmiops.workbench.notebooks;

import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;

public interface NotebooksService {

  String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";
  Pattern NOTEBOOK_PATTERN =
      Pattern.compile(NOTEBOOKS_WORKSPACE_DIRECTORY + "/[^/]+(\\.(?i)(ipynb))$");

  List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName);

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

  String getReadOnlyHtml(String workspaceNamespace, String workspaceName, String notebookName);
}
