package org.pmiops.workbench.environments;

import java.util.List;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentsAdminServiceImpl implements EnvironmentsAdminService {
  private final WorkspaceService workspaceService;

  @Autowired
  public EnvironmentsAdminServiceImpl(WorkspaceService workspaceService) {
    this.workspaceService = workspaceService;
  }

  /**
   * * Delete unshared environments (runtimes, apps, and disks) in the given workspaces. This method
   * will return the number of workspaces that failed to fetch workspace ACLs.
   *
   * @param workspaceNamespaces the namespaces of the workspaces to check for unshared environments
   * @return the number of workspaces that failed to fetch workspace ACLs
   */
  @Override
  public long deleteUnsharedWorkspaceEnvironmentsBatch(List<String> workspaceNamespaces) {
    return workspaceService.lookupWorkspacesByNamespace(workspaceNamespaces).size();
  }
}
