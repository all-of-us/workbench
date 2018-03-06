package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public interface WorkspaceService {

  public static final String PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER";

  public WorkspaceDao getDao();
  public FireCloudService getFireCloudService();
  public Workspace get(String ns, String firecloudName);
  public Workspace getByName(String ns, String name);
  public Workspace getRequired(String ns, String firecloudName);
  public Workspace getRequiredWithCohorts(String ns, String firecloudName);
  public Workspace saveWithLastModified(Workspace workspace);
  public List<Workspace> findForReview();
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved);
  public Workspace updateUserRoles(Workspace workspace, Set<WorkspaceUserRole> userRoleSet);
  public Workspace saveAndCloneCohorts(Workspace from, Workspace to);
  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId);
  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel requiredAccess);
}
