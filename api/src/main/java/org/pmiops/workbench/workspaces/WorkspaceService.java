package org.pmiops.workbench.workspaces;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceResponse;

/*
 * WorkspaceService is primarily an interface for coordinating the three Workspace models.
 *   - DbWorkspace - our representation of a Workspace
 *   - FirecloudWorkspace - Firecloud's concept of a Workspace
 *   - WorkspaceResponse - our API representation of Workspace which is a combination of the two models above
 *
 * Methods that need to coordinate changes between those models are a good candidate for what
 * should be added here. Most of these methods directly serve WorkspaceController.
 *
 * For example
 *   - creating the WorkspaceResponse model by fetching DbWorkspace and FirecloudWorkspace
 *   - deleting a workspace and making the changes to both our database and Firecloud
 *
 */
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

  DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to);

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  DbWorkspace setPublished(String workspaceNamespace, String firecloudName, boolean publish);

  List<DbUserRecentWorkspace> getRecentWorkspaces();

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);
}
