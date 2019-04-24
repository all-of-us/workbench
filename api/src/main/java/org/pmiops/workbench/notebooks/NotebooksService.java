package org.pmiops.workbench.notebooks;

import java.util.List;
import org.pmiops.workbench.model.FileDetail;

public interface NotebooksService {

  List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceName);

  FileDetail copyNotebook(String fromWorkspaceNamespace, String fromWorkspaceName, String fromNotebookName,
      String toWorkspaceNamespace, String toWorkspaceName, String newNotebookName);

  FileDetail cloneNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  void deleteNotebook(String workspaceNamespace, String workspaceName, String notebookName);

  FileDetail renameNotebook(String workspaceNamespace, String workspaceName, String notebookName, String newName);
}
