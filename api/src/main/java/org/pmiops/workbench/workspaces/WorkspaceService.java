package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface WorkspaceService {

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  void deleteWorkspace(DbWorkspace dbWorkspace);

  /*
   * This function will call the Google Cloud Billing API to set the given billing
   * account name to the given workspace. It will also update the billingAccountName
   * field on the workspace model.
   */
  void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName);

  void setResearchPurposeApproved(String ns, String firecloudName, boolean approved);

  DbWorkspace updateWorkspaceAcls(
      DbWorkspace workspace,
      Map<String, WorkspaceAccessLevel> userRoleMap,
      String registeredUsersGroup);

  DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to);

  Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName);

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  DbWorkspace setPublished(DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);
}
