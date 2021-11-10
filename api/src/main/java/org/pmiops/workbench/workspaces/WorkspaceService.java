package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceResponse;

/*
 * WorkspaceService is primarily an interface for coordinating the three Workspace models.
 *   - DbWorkspace - our representation of a Workspace
 *   - FirecloudWorkspaceDetails - Firecloud's concept of a Workspace
 *   - WorkspaceResponse - our API representation of Workspace which is a combination of the two models above
 *
 * Methods that need to coordinate changes between those models are a good candidate for what
 * should be added here. Most of these methods directly serve WorkspaceController.
 *
 * For example
 *   - creating the WorkspaceResponse model by fetching DbWorkspace and FirecloudWorkspaceDetails
 *   - deleting a workspace and making the changes to both our database and Firecloud
 *
 */
public interface WorkspaceService {

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId);

  /**
   * Retrieves a list of workspaces which <i>mostly</i> corresponds to the workspaces the user has
   * access to.
   *
   * <p>Includes:
   *
   * <ul>
   *   <li>Workspaces created by the user
   *   <li>Workspaces shared with the user as Owner, Writer, or Reader
   *   <li>"No Access" workspaces, which can happen in several ways:
   *       <ul>
   *         <li>Workspaces in an access tier that the user can't access
   *         <li>Workspaces previously shared but now unshared with the user
   *         <li>Workspaces which were previously published but are now unpublished
   *       </ul>
   * </ul>
   *
   * Excludes:
   *
   * <ul>
   *   <li>Deleted workspaces (WorkspaceActiveStatus.DELETED in the RW DB)
   *   <li>Published workspaces (as determined by the RW DB) if the user has Reader or No Access
   * </ul>
   */
  List<WorkspaceResponse> getWorkspaces();

  List<WorkspaceResponse> getPublishedWorkspaces();

  void deleteWorkspace(DbWorkspace dbWorkspace);

  boolean notebookTransferComplete(String workspaceNamespace, String workspaceId);

  /*
   * This function will call the Google Cloud Billing API to set the given billing
   * account name to the given workspace. It will also update the billingAccountName
   * field on the workspace model.
   */
  void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName);

  DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to);

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);

  Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> keySet);

  DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace);
}
