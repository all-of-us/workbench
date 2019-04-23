package org.pmiops.workbench.notebooks;

import org.pmiops.workbench.model.FileDetail;

public interface NotebooksService {

  FileDetail copyNotebook(String fromWorkspace, String fromWorkspaceName, String fromNotebookName,
      String toWorkspace, String toWorkspaceName, String toNotebookName);

}
