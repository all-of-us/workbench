package org.pmiops.workbench.actionaudit.auditors;

import javax.annotation.Nullable;
import org.pmiops.workbench.model.AccessReason;
import org.pmiops.workbench.model.AdminLockingRequest;

public interface AdminAuditor {
  void fireViewNotebookAction(
      String workspaceNamespace,
      String workspaceName,
      String notebookFilename,
      AccessReason accessReason);

  void fireLockWorkspaceAction(long workspaceId, AdminLockingRequest adminLockingRequest);

  void fireUnlockWorkspaceAction(long workspaceId);

  void firePublishWorkspaceAction(
      long workspaceId, String updatedCategory, @Nullable String prevCategoryIfAny);

  void fireUnpublishWorkspaceAction(long workspaceId, String prevCategory);
}
