package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Iterator;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.UserRoleList;


/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * TODO(RW-215) Add versioning to detect/prevent concurrent edits.
 */
@Service
public class WorkspaceService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access
   * it here.
   */
  public final WorkspaceDao dao;
  private final UserDao userDao;

  @Autowired
  public WorkspaceService(WorkspaceDao workspaceDao, UserDao userDao) {
    this.dao = workspaceDao;
    this.userDao = userDao;
  }

  public Workspace get(String ns, String id) {
    return dao.findByWorkspaceNamespaceAndFirecloudName(ns, id);
  }

  public Workspace getRequired(String ns, String id) {
    Workspace workspace = get(ns, id);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, id));
    }
    return workspace;
  }

  public List<Workspace> findForReview() {
    return dao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  }

  // FIXME @Version instead? Bean instantiation v. @Transactional?
  //@Transactional
  public void setResearchPurposeApproved(String ns, String id, boolean approved) {
    Workspace workspace = getRequired(ns, id);
    if (workspace.getReviewRequested() == null || !workspace.getReviewRequested()) {
      throw new BadRequestException(String.format(
          "No review requested for workspace %s/%s.", ns, id));
    }
    if (workspace.getApproved() != null) {
      throw new BadRequestException(String.format(
          "Workspace %s/%s already %s.",
          ns, id, workspace.getApproved() ? "approved" : "rejected"));
    }
    workspace.setApproved(approved);
    dao.save(workspace);
  }

  @Transactional
  public void updateUserRoles(String ns, String id, UserRoleList userRoleList) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = getRequired(
        ns, id);
    Iterator<WorkspaceUserRole> dbUserRoles = dbWorkspace.getWorkspaceUserRoles().iterator();
    while(dbUserRoles.hasNext()) {
      boolean resolved = false;
      WorkspaceUserRole currentUserRole = dbUserRoles.next();
      for (UserRole user : userRoleList.getItems()) {
        if (currentUserRole.getUser().getEmail().equals(user.getUser())) {
          currentUserRole.setRole(user.getRole());
          resolved = true;
          userRoleList.getItems().remove(user);
          break;
        }
      }
      if (!resolved) {
        dbWorkspace.getWorkspaceUserRoles().remove(currentUserRole);
      }
    }

    for (UserRole user : userRoleList.getItems()) {
      WorkspaceUserRole newUser = new WorkspaceUserRole();
      newUser.setUser(userDao.findUserByEmail(user.getUser()));
      newUser.setWorkspace(dbWorkspace);
      newUser.setRole(user.getRole());
      dbWorkspace.getWorkspaceUserRoles().add(newUser);
    }
  }
}
