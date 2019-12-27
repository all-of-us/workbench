package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface WorkspaceService {

  String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  WorkspaceDao getDao();

  Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId);

  FireCloudService getFireCloudService();

  DbWorkspace get(String ns, String firecloudName);

  // Returns the requested workspace looked up by workspace namespace (aka billing project name).
  // Only active workspaces are searched. Returns null if no active workspace is found.
  DbWorkspace getByNamespace(String workspaceNamespace);

  List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces();

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  DbWorkspace getRequired(String ns, String firecloudName);

  DbWorkspace getRequiredWithCohorts(String ns, String firecloudName);

  DbWorkspace saveWithLastModified(DbWorkspace workspace);

  /*
   * This function will check the workspace's billing status and throw a ForbiddenException
   * if it is inactive.
   *
   * There is no hard and fast rule on what operations should require active billing but
   * the general idea is that we should prevent operations that can either incur a non trivial
   * amount of Google Cloud computation costs (starting a notebook cluster) or increase the
   * monthly cost of the workspace (ex. creating GCS objects).
   */
  void validateActiveBilling(String workspaceNamespace, String workspaceId)
      throws ForbiddenException;

  List<DbWorkspace> findForReview();

  void setResearchPurposeApproved(String ns, String firecloudName, boolean approved);

  DbWorkspace updateWorkspaceAcls(
      DbWorkspace workspace,
      Map<String, WorkspaceAccessLevel> userRoleMap,
      String registeredUsersGroup);

  DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to);

  WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId);

  WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess);

  DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel);

  Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName);

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, FirecloudWorkspaceACLUpdate currentUpdate);

  DbWorkspace setPublished(DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(
      DbWorkspace workspace, long userId, Timestamp lastAccessDate);

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);

  boolean maybeDeleteRecentWorkspace(long workspaceId);
}
