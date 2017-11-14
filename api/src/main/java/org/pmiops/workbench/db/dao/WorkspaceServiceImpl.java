package org.pmiops.workbench.db.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pmiops.workbench.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;

/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * This needs to implement an interface to support Transactional
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  @Autowired private WorkspaceDao workspaceDao;

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access
   * it here.
   */
  @Override
  public WorkspaceDao getDao() {
    return workspaceDao;
  }
  @Override
  public void setDao(WorkspaceDao workspaceDao) {
    this.workspaceDao = workspaceDao;
  }
  @Override
  public Workspace get(String ns, String id) {
    return workspaceDao.findByWorkspaceNamespaceAndFirecloudName(ns, id);
  }
  @Override
  public Workspace getRequired(String ns, String id) {
    Workspace workspace = get(ns, id);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, id));
    }
    return workspace;
  }
  @Override
  public List<Workspace> findForReview() {
    return workspaceDao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  }

  @Override
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
    try {
      workspaceDao.save(workspace);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for workspace update", e);
      throw new ConflictException("Failed due to concurrent workspace modification");
    }
  }

  @Override
  @Transactional
  public void updateUserRoles(String ns, String id, Set<WorkspaceUserRole> userRoleSet) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = getRequired(
        ns, id);
    Map<Long, WorkspaceUserRole> userRoleMap = new HashMap<Long, WorkspaceUserRole>();
    for (WorkspaceUserRole userRole : userRoleSet) {
      userRole.setWorkspace(dbWorkspace);
      userRoleMap.put(userRole.getUser().getUserId(), userRole);
    }
    Iterator<WorkspaceUserRole> dbUserRoles = dbWorkspace.getWorkspaceUserRoles().iterator();
    while (dbUserRoles.hasNext()) {
      boolean resolved = false;
      WorkspaceUserRole currentUserRole = dbUserRoles.next();

      WorkspaceUserRole mapValue = userRoleMap.get(currentUserRole.getUser().getUserId());
      if (mapValue != null) {
        currentUserRole.setRole(mapValue.getRole());
        userRoleMap.remove(currentUserRole.getUser().getUserId());
      } else {
        dbUserRoles.remove();
      }
    }

    for (Map.Entry<Long, WorkspaceUserRole> remainingRole : userRoleMap.entrySet()) {
      dbWorkspace.getWorkspaceUserRoles().add(remainingRole.getValue());
    }
    // TODO(calbach): This save() is not technically necessary but included to
    // workaround RW-252. Remove either this, or @Transactional.
    workspaceDao.save(dbWorkspace);
  }
}
