package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.firecloud.FireCloudService;

public interface WorkspaceService {
  public WorkspaceDao getDao();
  public FireCloudService getFireCloudService();
  public Workspace get(String ns, String id);
  public Workspace getRequired(String ns, String id);
  public Workspace saveWithLastModified(Workspace workspace);
  public List<Workspace> findForReview();
  public void setResearchPurposeApproved(String ns, String id, boolean approved);
  public Workspace updateUserRoles(Workspace workspace, Set<WorkspaceUserRole> userRoleSet);
}
