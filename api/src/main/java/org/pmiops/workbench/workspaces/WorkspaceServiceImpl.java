package org.pmiops.workbench.workspaces;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CohortDataModel;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * <p>This needs to implement an interface to support Transactional
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

  private static final String FC_OWNER_ROLE = "OWNER";
  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  private CohortCloningService cohortCloningService;
  private ConceptSetService conceptSetService;
  private DataSetService dataSetService;
  private UserDao userDao;
  private Provider<User> userProvider;
  private UserRecentWorkspaceDao userRecentWorkspaceDao;
  private WorkspaceDao workspaceDao;

  private FireCloudService fireCloudService;
  private Clock clock;

  @Autowired
  public WorkspaceServiceImpl(
      Clock clock,
      CohortCloningService cohortCloningService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      UserDao userDao,
      Provider<User> userProvider,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      WorkspaceDao workspaceDao) {
    this.clock = clock;
    this.cohortCloningService = cohortCloningService;
    this.conceptSetService = conceptSetService;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.workspaceDao = workspaceDao;
  }

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access it
   * here.
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
  public DbWorkspace get(String ns, String firecloudName) {
    return workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
        ns,
        firecloudName,
        StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    return getWorkspacesAndPublicWorkspaces().stream()
        .filter(
            workspaceResponse ->
                workspaceResponse.getAccessLevel() == WorkspaceAccessLevel.OWNER
                    || workspaceResponse.getAccessLevel() == WorkspaceAccessLevel.WRITER
                    || !workspaceResponse.getWorkspace().getPublished())
        .collect(Collectors.toList());
  }

  @Override
  public List<WorkspaceResponse> getPublishedWorkspaces() {
    return getWorkspacesAndPublicWorkspaces().stream()
        .filter(workspaceResponse -> workspaceResponse.getWorkspace().getPublished())
        .collect(Collectors.toList());
  }

  @Override
  public List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces() {
    Map<String, org.pmiops.workbench.firecloud.model.WorkspaceResponse> fcWorkspaces =
        getFirecloudWorkspaces(ImmutableList.of("accessLevel", "workspace.workspaceId"));
    List<DbWorkspace> dbWorkspaces = workspaceDao.findAllByFirecloudUuidIn(fcWorkspaces.keySet());

    return dbWorkspaces.stream()
        .filter(
            dbWorkspace ->
                dbWorkspace.getWorkspaceActiveStatusEnum() == WorkspaceActiveStatus.ACTIVE)
        .map(
            dbWorkspace -> {
              String fcWorkspaceAccessLevel =
                  fcWorkspaces.get(dbWorkspace.getFirecloudUuid()).getAccessLevel();
              WorkspaceResponse currentWorkspace = new WorkspaceResponse();
              currentWorkspace.setWorkspace(WorkspaceConversionUtils.toApiWorkspace(dbWorkspace));
              currentWorkspace.setAccessLevel(
                  WorkspaceConversionUtils.toApiWorkspaceAccessLevel(fcWorkspaceAccessLevel));
              return currentWorkspace;
            })
        .collect(Collectors.toList());
  }

  @Transactional
  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = getRequired(workspaceNamespace, workspaceId);

    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();

    // This enforces access controls.
    fcResponse = fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
    fcWorkspace = fcResponse.getWorkspace();

    if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      // We don't expose PROJECT_OWNER in our API; just use OWNER.
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.OWNER);
    } else {
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel()));
      if (workspaceResponse.getAccessLevel() == null) {
        throw new ServerErrorException("Unsupported access level: " + fcResponse.getAccessLevel());
      }
    }
    workspaceResponse.setWorkspace(
        WorkspaceConversionUtils.toApiWorkspace(dbWorkspace, fcWorkspace));

    return workspaceResponse;
  }

  private Map<String, org.pmiops.workbench.firecloud.model.WorkspaceResponse>
      getFirecloudWorkspaces(List<String> fields) {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    return fireCloudService.getWorkspaces(fields).stream()
        .collect(
            Collectors.toMap(
                fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                fcWorkspace -> fcWorkspace));
  }

  @Override
  public Map<String, WorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName) {
    WorkspaceACL aclResp = fireCloudService.getWorkspaceAcl(workspaceNamespace, firecloudName);

    // Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
    // instead. Run this through a typed Gson conversion process to parse into the desired type.
    Type accessEntryType = new TypeToken<Map<String, WorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  /**
   * This is an internal method used by createWorkspace and cloneWorkspace endpoints, to check the
   * existence of ws name. Currently does not return a conflict if user is checking the name of a
   * deleted ws.
   */
  @Override
  public DbWorkspace getByName(String ns, String name) {
    return workspaceDao.findByWorkspaceNamespaceAndNameAndActiveStatus(
        ns, name, StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  @Override
  public DbWorkspace getRequired(String ns, String firecloudName) {
    DbWorkspace workspace = get(ns, firecloudName);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  @Transactional
  public DbWorkspace getRequiredWithCohorts(String ns, String firecloudName) {
    DbWorkspace workspace =
        workspaceDao.findByFirecloudNameAndActiveStatusWithEagerCohorts(
            ns,
            firecloudName,
            StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  public DbWorkspace saveWithLastModified(DbWorkspace workspace) {
    return saveWithLastModified(workspace, new Timestamp(clock.instant().toEpochMilli()));
  }

  private DbWorkspace saveWithLastModified(DbWorkspace workspace, Timestamp ts) {
    workspace.setLastModifiedTime(ts);
    try {
      return workspaceDao.save(workspace);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for workspace update", e);
      throw new ConflictException("Failed due to concurrent workspace modification");
    }
  }

  @Override
  public List<DbWorkspace> findForReview() {
    return workspaceDao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  }

  @Override
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved) {
    DbWorkspace workspace = getRequired(ns, firecloudName);
    if (workspace.getReviewRequested() == null || !workspace.getReviewRequested()) {
      throw new BadRequestException(
          String.format("No review requested for workspace %s/%s.", ns, firecloudName));
    }
    if (workspace.getApproved() != null) {
      throw new BadRequestException(
          String.format(
              "Workspace %s/%s already %s.",
              ns, firecloudName, workspace.getApproved() ? "approved" : "rejected"));
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspace.setApproved(approved);
    saveWithLastModified(workspace, now);
  }

  @Override
  public WorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, WorkspaceACLUpdate currentUpdate) {
    if (updatedAccess == WorkspaceAccessLevel.OWNER) {
      currentUpdate.setCanShare(true);
      currentUpdate.setCanCompute(true);
      currentUpdate.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    } else if (updatedAccess == WorkspaceAccessLevel.WRITER) {
      currentUpdate.setCanShare(false);
      currentUpdate.setCanCompute(true);
      currentUpdate.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
    } else if (updatedAccess == WorkspaceAccessLevel.READER) {
      currentUpdate.setCanShare(false);
      currentUpdate.setCanCompute(false);
      currentUpdate.setAccessLevel(WorkspaceAccessLevel.READER.toString());
    } else {
      currentUpdate.setCanShare(false);
      currentUpdate.setCanCompute(false);
      currentUpdate.setAccessLevel(WorkspaceAccessLevel.NO_ACCESS.toString());
    }
    return currentUpdate;
  }

  @Override
  public DbWorkspace updateWorkspaceAcls(
      DbWorkspace workspace,
      Map<String, WorkspaceAccessLevel> updatedAclsMap,
      String registeredUsersGroup) {
    // userRoleMap is a map of the new permissions for ALL users on the ws
    Map<String, WorkspaceAccessEntry> aclsMap =
        getFirecloudWorkspaceAcls(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());

    // Iterate through existing roles, update/remove them
    ArrayList<WorkspaceACLUpdate> updateACLRequestList = new ArrayList<>();
    Map<String, WorkspaceAccessLevel> toAdd = new HashMap<>(updatedAclsMap);
    for (Map.Entry<String, WorkspaceAccessEntry> entry : aclsMap.entrySet()) {
      String currentUserEmail = entry.getKey();
      WorkspaceAccessLevel updatedAccess = toAdd.get(currentUserEmail);
      if (updatedAccess != null) {
        WorkspaceACLUpdate currentUpdate = new WorkspaceACLUpdate();
        currentUpdate.setEmail(currentUserEmail);
        currentUpdate = updateFirecloudAclsOnUser(updatedAccess, currentUpdate);
        updateACLRequestList.add(currentUpdate);
        toAdd.remove(currentUserEmail);
      } else {
        // This is how to remove a user from the FireCloud ACL:
        // Pass along an update request with NO ACCESS as the given access level.
        // Note: do not do groups.  Unpublish will pass the specific NO_ACCESS acl
        // TODO [jacmrob] : have all users pass NO_ACCESS explicitly? Handle filtering on frontend?
        if (!currentUserEmail.equals(registeredUsersGroup)) {
          WorkspaceACLUpdate removedUser = new WorkspaceACLUpdate();
          removedUser.setEmail(currentUserEmail);
          removedUser = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, removedUser);
          updateACLRequestList.add(removedUser);
        }
      }
    }

    // Iterate through remaining new roles; add them
    for (Entry<String, WorkspaceAccessLevel> remainingRole : toAdd.entrySet()) {
      WorkspaceACLUpdate newUser = new WorkspaceACLUpdate();
      newUser.setEmail(remainingRole.getKey());
      newUser = updateFirecloudAclsOnUser(remainingRole.getValue(), newUser);
      updateACLRequestList.add(newUser);
    }
    WorkspaceACLUpdateResponseList fireCloudResponse =
        fireCloudService.updateWorkspaceACL(
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

    // Finally, keep OWNER and billing project users in lock-step. In Rawls, OWNER does not grant
    // canCompute on the workspace / billing project, nor does it grant the ability to grant
    // canCompute to other users. See RW-3009 for details.
    for (String email : Sets.union(updatedAclsMap.keySet(), aclsMap.keySet())) {
      String fromAccess =
          aclsMap.getOrDefault(email, new WorkspaceAccessEntry().accessLevel("")).getAccessLevel();
      WorkspaceAccessLevel toAccess =
          updatedAclsMap.getOrDefault(email, WorkspaceAccessLevel.NO_ACCESS);
      if (FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER != toAccess) {
        log.info(
            String.format(
                "removing user '%s' from billing project '%s'",
                email, workspace.getWorkspaceNamespace()));
        fireCloudService.removeUserFromBillingProject(email, workspace.getWorkspaceNamespace());
      } else if (!FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER == toAccess) {
        log.info(
            String.format(
                "adding user '%s' to billing project '%s'",
                email, workspace.getWorkspaceNamespace()));
        fireCloudService.addUserToBillingProject(email, workspace.getWorkspaceNamespace());
      }
    }

    return this.saveWithLastModified(workspace);
  }

  @Override
  @Transactional
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    // Save the workspace first to allocate an ID.
    to = workspaceDao.save(to);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(to.getCdrVersion());
    boolean cdrVersionChanged =
        from.getCdrVersion().getCdrVersionId() != to.getCdrVersion().getCdrVersionId();
    Map<Long, Long> fromCohortIdToToCohortId = new HashMap<>();
    for (CohortDataModel fromCohort : from.getCohorts()) {
      fromCohortIdToToCohortId.put(
          fromCohort.getCohortId(),
          cohortCloningService.cloneCohortAndReviews(fromCohort, to).getCohortId());
    }
    Map<Long, Long> fromConceptSetIdToToConceptSetId = new HashMap<>();
    for (ConceptSet fromConceptSet : conceptSetService.getConceptSets(from)) {
      fromConceptSetIdToToConceptSetId.put(
          fromConceptSet.getConceptSetId(),
          conceptSetService
              .cloneConceptSetAndConceptIds(fromConceptSet, to, cdrVersionChanged)
              .getConceptSetId());
    }
    for (DataSet dataSet : dataSetService.getDataSets(from)) {
      dataSetService.cloneDataSetToWorkspace(
          dataSet,
          to,
          fromCohortIdToToCohortId.entrySet().stream()
              .filter(cohortIdEntry -> dataSet.getCohortIds().contains(cohortIdEntry.getKey()))
              .map(Entry::getValue)
              .collect(Collectors.toSet()),
          fromConceptSetIdToToConceptSetId.entrySet().stream()
              .filter(conceptSetId -> dataSet.getConceptSetIds().contains(conceptSetId.getKey()))
              .map(Entry::getValue)
              .collect(Collectors.toSet()));
    }
    return to;
  }

  @Override
  public WorkspaceAccessLevel getWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId) {
    WorkspaceACL workspaceACL = fireCloudService.getWorkspaceAcl(workspaceNamespace, workspaceId);
    WorkspaceAccessEntry workspaceAccessEntry =
        Optional.of(workspaceACL.getAcl().get(userProvider.get().getEmail()))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format(
                            "Workspace %s/%s not found", workspaceNamespace, workspaceId)));
    final String userAccess = workspaceAccessEntry.getAccessLevel();

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
  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
    WorkspaceAccessLevel access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
    if (requiredAccess.compareTo(access) > 0) {
      throw new ForbiddenException(
          String.format(
              "You do not have sufficient permissions to access workspace %s/%s",
              workspaceNamespace, workspaceId));
    } else {
      return access;
    }
  }

  @Override
  public DbWorkspace getWorkspaceEnforceAccessLevelAndSetCdrVersion(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel workspaceAccessLevel) {
    enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, workspaceAccessLevel);
    DbWorkspace workspace = getRequired(workspaceNamespace, workspaceId);
    // Because we've already checked that the user has access to the workspace in question,
    // we don't need to check their membership in the authorization domain for the CDR version
    // associated with the workspace.
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(workspace.getCdrVersion());
    return workspace;
  }

  @Override
  public DbWorkspace findByWorkspaceId(long workspaceId) {
    DbWorkspace workspace = getDao().findOne(workspaceId);
    if (workspace == null
        || (workspace.getWorkspaceActiveStatusEnum() != WorkspaceActiveStatus.ACTIVE)) {
      throw new NotFoundException(String.format("Workspace %s not found.", workspaceId));
    }
    return workspace;
  }

  @Override
  public List<UserRole> convertWorkspaceAclsToUserRoles(
      Map<String, WorkspaceAccessEntry> rolesMap) {
    List<UserRole> userRoles = new ArrayList<>();
    for (Map.Entry<String, WorkspaceAccessEntry> entry : rolesMap.entrySet()) {
      // Filter out groups
      User user = userDao.findUserByEmail(entry.getKey());
      if (user == null) {
        log.log(Level.WARNING, "No user found for " + entry.getKey());
      } else {
        userRoles.add(WorkspaceConversionUtils.toApiUserRole(user, entry.getValue()));
      }
    }
    return userRoles.stream()
        .sorted(
            Comparator.comparing(UserRole::getRole).thenComparing(UserRole::getEmail).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public DbWorkspace setPublished(
      DbWorkspace workspace, String publishedWorkspaceGroup, boolean publish) {
    ArrayList<WorkspaceACLUpdate> updateACLRequestList = new ArrayList<>();
    WorkspaceACLUpdate currentUpdate = new WorkspaceACLUpdate();
    currentUpdate.setEmail(publishedWorkspaceGroup);

    if (publish) {
      currentUpdate = updateFirecloudAclsOnUser(WorkspaceAccessLevel.READER, currentUpdate);
      workspace.setPublished(true);
    } else {
      currentUpdate = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, currentUpdate);
      workspace.setPublished(false);
    }

    updateACLRequestList.add(currentUpdate);
    fireCloudService.updateWorkspaceACL(
        workspace.getWorkspaceNamespace(), workspace.getFirecloudName(), updateACLRequestList);

    return this.saveWithLastModified(workspace);
  }

  @Override
  @Transactional
  public List<UserRecentWorkspace> getRecentWorkspaces() {
    long userId = userProvider.get().getUserId();
    List<UserRecentWorkspace> userRecentWorkspaces =
        userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId);
    return pruneInaccessibleRecentWorkspaces(userRecentWorkspaces, userId);
  }

  private List<UserRecentWorkspace> pruneInaccessibleRecentWorkspaces(
      List<UserRecentWorkspace> recentWorkspaces, long userId) {
    List<DbWorkspace> dbWorkspaces =
        workspaceDao.findAllByWorkspaceIdIn(
            recentWorkspaces.stream()
                .map(UserRecentWorkspace::getWorkspaceId)
                .collect(Collectors.toList()));

    Set<Long> workspaceIdsToDelete =
        dbWorkspaces.stream()
            .filter(
                workspace -> {
                  try {
                    enforceWorkspaceAccessLevel(
                        workspace.getWorkspaceNamespace(),
                        workspace.getFirecloudName(),
                        WorkspaceAccessLevel.READER);
                  } catch (ForbiddenException | NotFoundException e) {
                    return true;
                  }
                  return false;
                })
            .map(DbWorkspace::getWorkspaceId)
            .collect(Collectors.toSet());

    if (!workspaceIdsToDelete.isEmpty()) {
      userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(userId, workspaceIdsToDelete);
    }

    return recentWorkspaces.stream()
        .filter(recentWorkspace -> !workspaceIdsToDelete.contains(recentWorkspace.getWorkspaceId()))
        .collect(Collectors.toList());
  }

  @Override
  public UserRecentWorkspace updateRecentWorkspaces(
      DbWorkspace workspace, long userId, Timestamp lastAccessDate) {
    Optional<UserRecentWorkspace> maybeRecentWorkspace =
        userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspace.getWorkspaceId(), userId);
    final UserRecentWorkspace matchingRecentWorkspace =
        maybeRecentWorkspace
            .map(
                recentWorkspace -> {
                  recentWorkspace.setLastAccessDate(lastAccessDate);
                  return recentWorkspace;
                })
            .orElseGet(
                () -> new UserRecentWorkspace(workspace.getWorkspaceId(), userId, lastAccessDate));
    userRecentWorkspaceDao.save(matchingRecentWorkspace);
    handleWorkspaceLimit(userId);
    return matchingRecentWorkspace;
  }

  @Override
  public UserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return updateRecentWorkspaces(
        workspace, userProvider.get().getUserId(), new Timestamp(clock.instant().toEpochMilli()));
  }

  @Transactional
  private void handleWorkspaceLimit(long userId) {
    List<UserRecentWorkspace> userRecentWorkspaces =
        userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId);

    ArrayList<Long> idsToDelete = new ArrayList<>();
    while (userRecentWorkspaces.size() > RECENT_WORKSPACE_COUNT) {
      idsToDelete.add(userRecentWorkspaces.get(userRecentWorkspaces.size() - 1).getWorkspaceId());
      userRecentWorkspaces.remove(userRecentWorkspaces.size() - 1);
    }
    userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(userId, idsToDelete);
  }

  @Override
  /** Returns true if anything was deleted from user_recent_workspaces, false if nothing was */
  public boolean maybeDeleteRecentWorkspace(long workspaceId) {
    long userId = userProvider.get().getUserId();
    Optional<UserRecentWorkspace> maybeRecentWorkspace =
        userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspaceId, userId);
    if (maybeRecentWorkspace.isPresent()) {
      userRecentWorkspaceDao.delete(maybeRecentWorkspace.get());
      return true;
    } else {
      return false;
    }
  }
}
