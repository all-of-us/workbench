package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface WorkspaceService {

  // TODO eric: move this into auth
  String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId);

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  DbWorkspace getRequired(String ns, String firecloudName);

  DbWorkspace getRequiredWithCohorts(String ns, String firecloudName);

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

  DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel);

  Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName);

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, FirecloudWorkspaceACLUpdate currentUpdate);

  DbWorkspace setPublished(DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);

}
