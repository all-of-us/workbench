package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.db.model.DbWorkspace
import org.pmiops.workbench.model.Workspace

interface WorkspaceAuditor {
    fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long)

    fun fireEditAction(
        previousWorkspace: Workspace?,
        editedWorkspace: Workspace?,
        workspaceId: Long
    )

    fun fireDeleteAction(dbWorkspace: DbWorkspace)

    fun fireDuplicateAction(
        sourceWorkspaceId: Long,
        destinationWorkspaceId: Long,
        destinationWorkspace: Workspace
    )

    fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>)
}
