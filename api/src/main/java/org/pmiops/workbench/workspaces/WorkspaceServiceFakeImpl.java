package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.tanagra.ApiException;
import org.pmiops.workbench.tanagra.model.Study;

public class WorkspaceServiceFakeImpl implements WorkspaceService {

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
  public String getPublishedWorkspacesGroupEmail() {
    return null;
  }

  @Override
  public void deleteWorkspace(DbWorkspace dbWorkspace) {}

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {}

  @Override
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    return null;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    return null;
  }

  @Override
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    return null;
  }

  @Override
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return null;
  }

  @Override
  public Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> keySet) {
    return null;
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceId) {
    return false;
  }

  @Override
  public DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace)
      throws NotFoundException {
    return null;
  }

  @Override
  public Study createTanagraStudy(String workspaceNamespace, String workspaceName)
      throws ApiException {
    return null;
  }
}
