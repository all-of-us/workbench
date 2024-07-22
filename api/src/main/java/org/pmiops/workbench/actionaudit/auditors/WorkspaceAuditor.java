package org.pmiops.workbench.actionaudit.auditors;

import java.util.Map;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;

/** Auditor service which handles collecting audit logs for workspace-related actions. */
public interface WorkspaceAuditor {
  /** Fires an audit log event for creating a workspace. */
  void fireCreateAction(Workspace createdWorkspace, long dbWorkspaceId);

  /** Fires an audit log event for editing a workspace. */
  void fireEditAction(Workspace previousWorkspace, Workspace editedWorkspace, long workspaceId);

  /** Fires an audit log event for deleting a workspace. */
  void fireDeleteAction(DbWorkspace dbWorkspace);

  /** Fires an audit log event for duplicating a workspace. */
  void fireDuplicateAction(
      long sourceWorkspaceId, long destinationWorkspaceId, Workspace destinationWorkspace);

  /** Fires an audit log event for collaborating on a workspace. */
  void fireCollaborateAction(long sourceWorkspaceId, Map<Long, String> aclStringsByUserId);

  /** Fires an audit log event for publishing a workspace */
  void firePublishAction(long workspaceId);
}
