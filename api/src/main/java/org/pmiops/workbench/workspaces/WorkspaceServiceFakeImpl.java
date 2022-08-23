package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceResponse;

public class WorkspaceServiceFakeImpl implements WorkspaceService {

  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    return null;
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    return null;
  }

  @Override
  public List<WorkspaceResponse> getPublishedWorkspaces() {
    return null;
  }

  @Override
  public boolean deleteWorkspace(DbWorkspace dbWorkspace) {
    return true;
  }

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {}

  @Override
  public void forceDeleteWorkspace(DbWorkspace dbWorkspace) {}

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return null;
  }

  @Override
  public Set<DbWorkspace> getActiveWorkspacesForUser(DbUser user) {
    return null;
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceId) {
    return false;
  }
}
