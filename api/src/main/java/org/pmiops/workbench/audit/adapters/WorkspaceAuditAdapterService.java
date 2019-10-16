package org.pmiops.workbench.audit.adapters;

import org.pmiops.workbench.model.Workspace;

public interface WorkspaceAuditAdapterService {
  void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId);

  void fireDeleteAction(org.pmiops.workbench.db.model.Workspace dbWorkspace);

  void fireDuplicateAction(long sourceWorkspaceId, long destinationWorkspaceId);
}
