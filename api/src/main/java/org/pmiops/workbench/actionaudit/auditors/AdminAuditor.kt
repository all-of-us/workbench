package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.model.AccessReason

interface AdminAuditor {
    fun fireViewNotebookAction(workspaceNamespace: String, workspaceName: String, notebookFilename: String, accessReason: AccessReason)
}
