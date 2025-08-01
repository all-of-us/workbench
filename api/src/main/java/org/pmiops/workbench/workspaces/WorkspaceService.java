package org.pmiops.workbench.workspaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.tanagra.model.Cohort;
import org.pmiops.workbench.tanagra.model.CohortList;
import org.pmiops.workbench.tanagra.model.FeatureSet;
import org.pmiops.workbench.tanagra.model.FeatureSetList;

/*
 * WorkspaceService is primarily an interface for coordinating the three Workspace models.
 *   - DbWorkspace - our representation of a Workspace
 *   - RawlsWorkspaceDetails - Firecloud's concept of a Workspace
 *   - WorkspaceResponse - our API representation of Workspace which is a combination of the two models above
 *
 * Methods that need to coordinate changes between those models are a good candidate for what
 * should be added here. Most of these methods directly serve WorkspaceController.
 *
 * For example
 *   - creating the WorkspaceResponse model by fetching DbWorkspace and RawlsWorkspaceDetails
 *   - deleting a workspace and making the changes to both our database and Firecloud
 *
 */
public interface WorkspaceService {

  WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceTerraName);

  boolean notebookTransferComplete(String workspaceNamespace, String workspaceTerraName);

  List<WorkspaceResponse> listWorkspaces();

  List<String> getActiveWorkspaceNamespacesAsService();

  List<String> getOrphanedWorkspaceNamespacesAsService();

  /**
   * Get all Featured workspaces from the DB.
   *
   * @return List of all Featured workspaces
   */
  List<WorkspaceResponse> getFeaturedWorkspaces();

  /**
   * Return the email associated with the group that we use to indicate that a workspace is
   * published. (implementation detail: it's the RT auth domain group email)
   */
  String getPublishedWorkspacesGroupEmail();

  void deleteWorkspace(DbWorkspace dbWorkspace);

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

  List<DbWorkspace> lookupWorkspacesByNamespace(Collection<String> workspaceNamespaces);

  /**
   * This call will create a Study in the Tanagra application. A Tanagra Study is equivalent to a
   * AoU workspace.
   */
  void createTanagraStudy(String workspaceNamespace, String workspaceName);

  CohortList listTanagraCohorts(String workspaceNamespace, Integer offset, Integer limit);

  FeatureSetList listTanagraFeatureSets(String workspaceNamespace, Integer offset, Integer limit);

  void cloneTanagraCohort(
      Cohort cohort, String fromWorkspaceNamespace, String toWorkspaceNamespace);

  void cloneTanagraFeatureSet(
      FeatureSet featureSet, String fromWorkspaceNamespace, String toWorkspaceNamespace);

  void publishCommunityWorkspace(DbWorkspace workspace);

  List<DbUser> getWorkspaceOwnerList(DbWorkspace dbWorkspace);

  RawlsWorkspaceDetails createWorkspace(Workspace workspace, DbCdrVersion cdrVersion);

  RawlsWorkspaceDetails cloneWorkspace(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      Workspace toWorkspace,
      DbCdrVersion cdrVersion);
}
