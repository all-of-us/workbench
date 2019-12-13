package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.monitoring.GaugeDataCollector;

public interface WorkspaceService {

  String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  WorkspaceDao getDao();

  Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId);

  FireCloudService getFireCloudService();

  DbWorkspace get(String ns, String firecloudName);

  List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces();

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  DbWorkspace getRequired(String ns, String firecloudName);

  DbWorkspace getRequiredWithCohorts(String ns, String firecloudName);

  DbWorkspace saveWithLastModified(DbWorkspace workspace);

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

  List<UserRole> convertWorkspaceAclsToUserRoles(
      Map<String, FirecloudWorkspaceAccessEntry> rolesMap);

  FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, FirecloudWorkspaceACLUpdate currentUpdate);

  DbWorkspace setPublished(DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(
      DbWorkspace workspace, long userId, Timestamp lastAccessDate);

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);

  boolean maybeDeleteRecentWorkspace(long workspaceId);
}
