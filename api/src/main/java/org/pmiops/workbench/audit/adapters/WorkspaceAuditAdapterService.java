package org.pmiops.workbench.audit.adapters;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;

public interface WorkspaceAuditAdapterService {
  void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId);

  void fireDeleteAction(DbWorkspace dbWorkspace);
}
