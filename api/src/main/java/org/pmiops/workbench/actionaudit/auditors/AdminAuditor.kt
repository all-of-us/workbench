package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.model.AccessReason
import org.pmiops.workbench.model.AdminLockingRequest

interface AdminAuditor {
    fun fireViewNotebookAction(
        workspaceNamespace: String,
        workspaceName: String,
        notebookFilename: String,
        accessReason: AccessReason,
    )

    fun fireLockWorkspaceAction(
        workspaceId: Long,
        adminLockingRequest: AdminLockingRequest,
    )

    fun fireUnlockWorkspaceAction(workspaceId: Long)
}
