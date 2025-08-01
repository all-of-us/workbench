package org.pmiops.workbench.impersonation;

import java.util.List;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.workspaces.WorkspaceService}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedWorkspaceService {
  // return a list of workspaces owned by the user
  List<WorkspaceResponse> getOwnedWorkspaces(String username);

  // return a list of workspaces which are present in Rawls as owned by the user
  // but not present (or deleted) in the AoU RW DB
  List<RawlsWorkspaceListResponse> getOwnedWorkspacesOrphanedInRawls(String username);

  void deleteWorkspace(
      String username,
      String workspaceNamespace,
      String workspaceTerraName,
      boolean deleteBillingProjects);

  void deleteOrphanedRawlsWorkspace(
      String username,
      String workspaceNamespace,
      String googleProject,
      String workspaceTerraName,
      boolean deleteBillingProjects);

  void cleanupWorkspace(String workspaceNamespace, String lastModifiedBy);
}
