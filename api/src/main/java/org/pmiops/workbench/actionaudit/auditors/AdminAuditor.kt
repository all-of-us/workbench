package org.pmiops.workbench.actionaudit.auditors

interface AdminAuditor {
    fun fireViewNotebookAction(workspaceNamespace: String, workspaceName: String, notebookFilename: String, accessReason: String)
}
