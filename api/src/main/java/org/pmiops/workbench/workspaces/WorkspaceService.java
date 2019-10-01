package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface WorkspaceService {

  String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  WorkspaceDao getDao();

  Workspace findByWorkspaceId(long workspaceId);

  FireCloudService getFireCloudService();

  Workspace get(String ns, String firecloudName);

  List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces();

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  Workspace getByName(String ns, String name);

  Workspace getRequired(String ns, String firecloudName);

  Workspace getRequiredWithCohorts(String ns, String firecloudName);

  Workspace saveWithLastModified(Workspace workspace);

  List<Workspace> findForReview();

  void setResearchPurposeApproved(String ns, String firecloudName, boolean approved);

  Workspace updateWorkspaceAcls(
      Workspace workspace,
      Map<String, WorkspaceAccessLevel> userRoleMap,
      String registeredUsersGroup);

  Workspace saveAndCloneCohortsAndConceptSets(Workspace from, Workspace to);

  WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId);

  WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess);

  Workspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel);

  Map<String, WorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName);

  List<UserRole> convertWorkspaceAclsToUserRoles(Map<String, WorkspaceAccessEntry> rolesMap);

  WorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, WorkspaceACLUpdate currentUpdate);

  Workspace setPublished(Workspace workspace, String publishedWorkspaceGroup, boolean publish);

  List<UserRecentWorkspace> getRecentWorkspaces();

  UserRecentWorkspace updateRecentWorkspaces(
      long workspaceId, long userId, Timestamp lastAccessDate);

  UserRecentWorkspace updateRecentWorkspaces(long workspaceId);

  boolean maybeDeleteRecentWorkspace(long workspaceId);

  List<RecentWorkspace> buildRecentWorkspaceList(List<UserRecentWorkspace> userRecentWorkspaces);

  RecentWorkspace buildRecentWorkspace(
      org.pmiops.workbench.db.model.Workspace dbWorkspace,
      UserRecentWorkspace userRecentWorkspace,
      WorkspaceAccessLevel accessLevel);
}
