package org.pmiops.workbench.db.dao;

import java.util.ArrayList;
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
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;


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
  @Autowired private FireCloudService fireCloudService;

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
  public FireCloudService getFireCloudService() {
    return fireCloudService;
  }
  @Override
  public void setFireCloudService(FireCloudService fireCloudService) {
    this.fireCloudService = fireCloudService;
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
    ArrayList<WorkspaceACLUpdate> updateACLRequestList = new ArrayList<WorkspaceACLUpdate>();
    Iterator<WorkspaceUserRole> dbUserRoles = dbWorkspace.getWorkspaceUserRoles().iterator();
    while (dbUserRoles.hasNext()) {
      boolean resolved = false;
      WorkspaceUserRole currentUserRole = dbUserRoles.next();

      WorkspaceUserRole mapValue = userRoleMap.get(currentUserRole.getUser().getUserId());
      if (mapValue != null) {
        currentUserRole.setRole(mapValue.getRole());
        userRoleMap.remove(currentUserRole.getUser().getUserId());
      } else {
        // This is how to remove a user from the FireCloud ACL:
        // Pass along an update request with NO ACCESS as the given access level.
        WorkspaceACLUpdate removedUser = new WorkspaceACLUpdate();
        removedUser.setEmail(currentUserRole.getUser().getEmail());
        removedUser.setCanCompute(false);
        removedUser.setCanShare(false);
        removedUser.setAccessLevel(WorkspaceAccessLevel.NO_ACCESS.toString());
        updateACLRequestList.add(removedUser);
        dbUserRoles.remove();
      }
    }

    for (Map.Entry<Long, WorkspaceUserRole> remainingRole : userRoleMap.entrySet()) {
      dbWorkspace.getWorkspaceUserRoles().add(remainingRole.getValue());
    }

    for(WorkspaceUserRole currentWorkspaceUser : dbWorkspace.getWorkspaceUserRoles()) {
      WorkspaceACLUpdate currentUpdate = new WorkspaceACLUpdate();
      currentUpdate.setEmail(currentWorkspaceUser.getUser().getEmail());
      currentUpdate.setCanCompute(false);
      if (currentWorkspaceUser.getRole() == WorkspaceAccessLevel.OWNER) {
        currentUpdate.setCanShare(true);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
      } else if (currentWorkspaceUser.getRole() == WorkspaceAccessLevel.WRITER) {
        currentUpdate.setCanShare(false);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
      } else {
        currentUpdate.setCanShare(false);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.READER.toString());
      }
      updateACLRequestList.add(currentUpdate);
    }
    try {
      WorkspaceACLUpdateResponseList fireCloudResponse = fireCloudService.updateWorkspaceACL(ns, id, updateACLRequestList);
      if (fireCloudResponse.getUsersNotFound().size() != 0) {
        String usersNotFound = "";
        for (int i = 0; i < fireCloudResponse.getUsersNotFound().size(); i++) {
          if (i > 0) {
            usersNotFound += ", ";
          }
          usersNotFound += fireCloudResponse.getUsersNotFound().get(i).getEmail();
        }
        throw new BadRequestException(usersNotFound);
      }
      // TODO(calbach): This save() is not technically necessary but included to
      // workaround RW-252. Remove either this, or @Transactional.
      workspaceDao.save(dbWorkspace);
    } catch(org.pmiops.workbench.firecloud.ApiException e) {
      if (e.getCode() == 400) {
        throw new BadRequestException(e.getResponseBody());
      } else if (e.getCode() == 404) {
        throw new NotFoundException("Workspace not found.");
      } else if (e.getCode() == 500) {
        throw new ServerErrorException(e);
      } else {
        throw new ServerUnavailableException(e);
      }
    }
  }
}
