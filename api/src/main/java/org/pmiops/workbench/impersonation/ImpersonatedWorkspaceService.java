package org.pmiops.workbench.impersonation;

import java.util.List;
import org.pmiops.workbench.model.WorkspaceResponse;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.workspaces.WorkspaceService}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedWorkspaceService {
  List<WorkspaceResponse> getOwnedWorkspaces(String username);

  void deleteWorkspace(String username, String wsNamespace, String wsId);
}
