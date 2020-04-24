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
import org.pmiops.workbench.model.WorkspaceDetailsHeavy;
import org.pmiops.workbench.model.WorkspaceResponse;

public class WorkspaceServiceFakeImpl implements WorkspaceService {

  @Override
  public WorkspaceDao getDao() {
    return null;
  }

  @Override
  public Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId) {
    return Optional.empty();
  }

  @Override
  public FireCloudService getFireCloudService() {
    return null;
  }

  @Override
  public DbWorkspace get(String ns, String firecloudName) {
    return null;
  }

  @Override
  public Optional<DbWorkspace> getByNamespace(String workspaceNamespace) {
    return Optional.empty();
  }

  @Override
  public List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces() {
    return null;
  }

  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace) throws NotFoundException {
    return null;
  }

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
  public DbWorkspace getRequired(String ns, String firecloudName) {
    return null;
  }

  @Override
  public DbWorkspace getRequiredWithCohorts(String ns, String firecloudName) {
    return null;
  }

  @Override
  public DbWorkspace saveWithLastModified(DbWorkspace workspace) {
    return null;
  }

  @Override
  public void deleteWorkspace(DbWorkspace dbWorkspace) {

  }

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {

  }

  @Override
  public void validateActiveBilling(String workspaceNamespace, String workspaceId)
      throws ForbiddenException {

  }

  @Override
  public List<DbWorkspace> findForReview() {
    return null;
  }

  @Override
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved) {

  }

  @Override
  public DbWorkspace updateWorkspaceAcls(DbWorkspace workspace,
      Map<String, WorkspaceAccessLevel> userRoleMap, String registeredUsersGroup) {
    return null;
  }

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace,
      String workspaceId) {
    return null;
  }

  @Override
  public WorkspaceAccessLevel enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
    return null;
  }

  @Override
  public DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel workspaceAccessLevel) {
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
  public FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(WorkspaceAccessLevel updatedAccess,
      FirecloudWorkspaceACLUpdate currentUpdate) {
    return null;
  }

  @Override
  public DbWorkspace setPublished(DbWorkspace workspace, String publishedWorkspaceGroup,
      boolean publish) {
    return null;
  }

  @Override
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace, long userId,
      Timestamp lastAccessDate) {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return null;
  }

  @Override
  public boolean maybeDeleteRecentWorkspace(long workspaceId) {
    return false;
  }

  @Override
  public List<WorkspaceDetailsHeavy> getWorkspaceDetailsHeavy(String workspaceNamespace) {
    return null;
  }
}
