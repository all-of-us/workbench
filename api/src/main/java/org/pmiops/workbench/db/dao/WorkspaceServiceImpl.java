package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * This needs to implement an interface to support Transactional
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private CohortCloningService cohortCloningService;
  @Autowired private ConceptSetService conceptSetService;
  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private FireCloudService fireCloudService;
  @Autowired private Clock clock;

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access
   * it here.
   */
  @Override
  public WorkspaceDao getDao() {
    return workspaceDao;
  }
  @Override
  public FireCloudService getFireCloudService() {
    return fireCloudService;
  }
  @Override
  public Workspace get(String ns, String firecloudName) {
    return workspaceDao.findByWorkspaceNamespaceAndFirecloudName(ns, firecloudName);
  }

  @Override
  public Workspace getByName(String ns, String name) {
    return workspaceDao.findByWorkspaceNamespaceAndName(ns, name);
  }

  @Override
  public Workspace getRequired(String ns, String firecloudName) {
    Workspace workspace = get(ns, firecloudName);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  @Transactional
  public Workspace getRequiredWithCohorts(String ns, String firecloudName) {
    Workspace workspace = workspaceDao.findByFirecloudWithEagerCohorts(ns, firecloudName);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  public Workspace saveWithLastModified(Workspace workspace) {
    return saveWithLastModified(workspace, new Timestamp(clock.instant().toEpochMilli()));
  }

  private Workspace saveWithLastModified(Workspace workspace, Timestamp ts) {
    workspace.setLastModifiedTime(ts);
    try {
      return workspaceDao.save(workspace);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for workspace update", e);
      throw new ConflictException("Failed due to concurrent workspace modification");
    }
  }

  @Override
  public List<Workspace> findForReview() {
    return workspaceDao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  }

  @Override
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved) {
    Workspace workspace = getRequired(ns, firecloudName);
    if (workspace.getReviewRequested() == null || !workspace.getReviewRequested()) {
      throw new BadRequestException(String.format(
          "No review requested for workspace %s/%s.", ns, firecloudName));
    }
    if (workspace.getApproved() != null) {
      throw new BadRequestException(String.format(
          "Workspace %s/%s already %s.",
          ns, firecloudName, workspace.getApproved() ? "approved" : "rejected"));
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspace.setApproved(approved);
    saveWithLastModified(workspace, now);
  }

  @Override
  public Workspace updateUserRoles(Workspace workspace, Set<WorkspaceUserRole> userRoleSet) {
    Map<Long, WorkspaceUserRole> userRoleMap = new HashMap<Long, WorkspaceUserRole>();
    for (WorkspaceUserRole userRole : userRoleSet) {
      userRole.setWorkspace(workspace);
      userRoleMap.put(userRole.getUser().getUserId(), userRole);
    }
    ArrayList<WorkspaceACLUpdate> updateACLRequestList = new ArrayList<WorkspaceACLUpdate>();
    Iterator<WorkspaceUserRole> dbUserRoles = workspace.getWorkspaceUserRoles().iterator();
    while (dbUserRoles.hasNext()) {
      WorkspaceUserRole currentUserRole = dbUserRoles.next();

      WorkspaceUserRole mapValue = userRoleMap.get(currentUserRole.getUser().getUserId());
      if (mapValue != null) {
        currentUserRole.setRoleEnum(mapValue.getRoleEnum());
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

    for (Entry<Long, WorkspaceUserRole> remainingRole : userRoleMap.entrySet()) {
      workspace.getWorkspaceUserRoles().add(remainingRole.getValue());
    }

    for(WorkspaceUserRole currentWorkspaceUser : workspace.getWorkspaceUserRoles()) {
      WorkspaceACLUpdate currentUpdate = new WorkspaceACLUpdate();
      currentUpdate.setEmail(currentWorkspaceUser.getUser().getEmail());
      currentUpdate.setCanCompute(false);
      WorkspaceAccessLevel access = currentWorkspaceUser.getRoleEnum();
      if (access == WorkspaceAccessLevel.OWNER) {
        currentUpdate.setCanShare(true);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
      } else if (access == WorkspaceAccessLevel.WRITER) {
        currentUpdate.setCanShare(false);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
      } else {
        currentUpdate.setCanShare(false);
        currentUpdate.setAccessLevel(WorkspaceAccessLevel.READER.toString());
      }
      updateACLRequestList.add(currentUpdate);
    }
    WorkspaceACLUpdateResponseList fireCloudResponse = fireCloudService.updateWorkspaceACL(
        workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), updateACLRequestList);
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
    return this.saveWithLastModified(workspace);
  }

  @Override
  @Transactional
  public Workspace saveAndCloneCohortsAndConceptSets(Workspace from, Workspace to) {
    // Save the workspace first to allocate an ID.
    Workspace saved = workspaceDao.save(to);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(saved.getCdrVersion());
    boolean cdrVersionChanged = from.getCdrVersion().getCdrVersionId() !=
        to.getCdrVersion().getCdrVersionId();
    for (Cohort fromCohort : from.getCohorts()) {
      cohortCloningService.cloneCohortAndReviews(fromCohort, to);
    }
    for (ConceptSet conceptSet : conceptSetService.getConceptSets(from)) {
      conceptSetService.cloneConceptSetAndConceptIds(conceptSet, to, cdrVersionChanged);
    }
    return saved;
  }

  @Override
  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId) {
    String userAccess = fireCloudService.getWorkspace(
          workspaceNamespace, workspaceId).getAccessLevel();
    if (userAccess.equals(PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    }
    WorkspaceAccessLevel result = WorkspaceAccessLevel.fromValue(userAccess);
    if (result == null) {
      throw new ServerErrorException("Unrecognized access level: " + userAccess);
    }
    return result;
  }

  @Override
  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel requiredAccess) {
    WorkspaceAccessLevel access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
    if (requiredAccess.compareTo(access) > 0) {
      throw new ForbiddenException(String.format("You do not have sufficient permissions to access workspace %s/%s",
          workspaceNamespace, workspaceId));
    } else {
      return access;
    }
  }

  @Override
  public Workspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(String workspaceNamespace,
      String workspaceId, WorkspaceAccessLevel workspaceAccessLevel) {
    enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, workspaceAccessLevel);
    Workspace workspace = getRequired(workspaceNamespace, workspaceId);
    // Because we've already checked that the user has access to the workspace in question,
    // we don't need to check their membership in the authorization domain for the CDR version
    // associated with the workspace.
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(workspace.getCdrVersion());
    return workspace;
  }

  @Override
  public Workspace findByWorkspaceId(long workspaceId) {
    return getDao().findOne(workspaceId);
  }
}
