package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
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
  public void deleteWorkspace(DbWorkspace dbWorkspace) {}

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {}

  @Override
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved) {}

  @Override
  public DbWorkspace updateWorkspaceAcls(
      DbWorkspace workspace,
      Map<String, WorkspaceAccessLevel> userRoleMap,
      String registeredUsersGroup) {
    return null;
  }

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public DbWorkspace setPublished(
      DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish) {
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
}
