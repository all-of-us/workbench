package org.pmiops.workbench.audit.adapters

import org.pmiops.workbench.model.Workspace

interface WorkspaceAuditAdapterService {
    fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long)

    fun fireDeleteAction(dbWorkspace: org.pmiops.workbench.db.model.Workspace)

    fun fireDuplicateAction(
        sourceWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace,
        destinationWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace
    )

    fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>)
}
