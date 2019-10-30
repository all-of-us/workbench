package org.pmiops.workbench.audit.adapters;

import java.util.Map;
import org.pmiops.workbench.model.Workspace;

public interface WorkspaceAuditAdapterService {
  void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId);

  void fireDeleteAction(org.pmiops.workbench.db.model.Workspace dbWorkspace);

  void fireDuplicateAction(
      org.pmiops.workbench.db.model.Workspace sourceWorkspaceDbModel,
      org.pmiops.workbench.db.model.Workspace destinationWorkspaceDbModel);

  void fireCollaborateAction(long sourceWorkspaceId, Map<Long, String> aclStringsByUserId);
}
