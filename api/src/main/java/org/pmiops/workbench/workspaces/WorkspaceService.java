package org.pmiops.workbench.workspaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceResponse;

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


  List<WorkspaceResponse> listWorkspaces();

  /**
   * Return the email associated with the group that we use to indicate that a workspace is
   * published. (implementation detail: it's the RT auth domain group email)
   */
  String getPublishedWorkspacesGroupEmail();

  List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName);

  DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace);

  Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> keySet);

  DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace);

  List<DbWorkspace> lookupWorkspacesByNamespace(Collection<String> workspaceNamespaces);

  List<DbUser> getWorkspaceOwnerList(DbWorkspace dbWorkspace);
}
