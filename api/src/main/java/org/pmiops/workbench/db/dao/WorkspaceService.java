package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pmiops.workbench.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;

public interface WorkspaceService {
  public WorkspaceDao getDao();
  public void setDao(WorkspaceDao workspaceDao);
  public Workspace get(String ns, String id);
  public Workspace getRequired(String ns, String id);
  public List<Workspace> findForReview();
  public void setResearchPurposeApproved(String ns, String id, boolean approved);
  public void updateUserRoles(String ns, String id, Set<WorkspaceUserRole> userRoleSet);
}
