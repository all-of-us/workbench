package org.pmiops.workbench.audit.adapters;

import java.util.Map;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;

public interface WorkspaceAuditAdapterService {
  void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId);

  void fireDeleteAction(DbWorkspace dbWorkspace);

  void fireDuplicateAction(
      DbWorkspace sourceWorkspaceDbModel,
      DbWorkspace destinationWorkspaceDbModel);

  void fireCollaborateAction(long sourceWorkspaceId, Map<Long, String> aclStringsByUserId);
}
