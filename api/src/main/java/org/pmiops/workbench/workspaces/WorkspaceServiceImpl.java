package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.billing.GoogleApisConfig.END_USER_CLOUD_BILLING;
import static org.pmiops.workbench.billing.GoogleApisConfig.SERVICE_ACCOUNT_CLOUD_BILLING;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.Cloudbilling.Projects.UpdateBillingInfo;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DbWorkspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * <p>This needs to implement an interface to support Transactional
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService, GaugeDataCollector {
  private static final Logger logger = Logger.getLogger(WorkspaceServiceImpl.class.getName());

  private static final String FC_OWNER_ROLE = "OWNER";
  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  private final Provider<Cloudbilling> endUserCloudbillingProvider;
  private final Provider<Cloudbilling> serviceAccountCloudbillingProvider;
  private final CohortCloningService cohortCloningService;
  private final ConceptSetService conceptSetService;
  private final DataSetService dataSetService;
  private final UserDao userDao;
  private final Provider<DbUser> userProvider;
  private final UserRecentWorkspaceDao userRecentWorkspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceDao workspaceDao;
  private final ManualWorkspaceMapper manualWorkspaceMapper;
  private final FreeTierBillingService freeTierBillingService;

  private FireCloudService fireCloudService;
  private Clock clock;

  @Autowired
  public WorkspaceServiceImpl(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> endUserCloudbillingProvider,
      @Qualifier(SERVICE_ACCOUNT_CLOUD_BILLING)
          Provider<Cloudbilling> serviceAccountCloudbillingProvider,
      Clock clock,
      CohortCloningService cohortCloningService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      UserDao userDao,
      Provider<DbUser> userProvider,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceDao workspaceDao,
      ManualWorkspaceMapper manualWorkspaceMapper,
      FreeTierBillingService freeTierBillingService) {
    this.endUserCloudbillingProvider = endUserCloudbillingProvider;
    this.serviceAccountCloudbillingProvider = serviceAccountCloudbillingProvider;
    this.clock = clock;
    this.cohortCloningService = cohortCloningService;
    this.conceptSetService = conceptSetService;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
    this.manualWorkspaceMapper = manualWorkspaceMapper;
    this.freeTierBillingService = freeTierBillingService;
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
        DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
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
    Map<String, FirecloudWorkspaceResponse> fcWorkspaces =
        getFirecloudWorkspaces(ImmutableList.of("accessLevel", "workspace.workspaceId"));
    List<DbWorkspace> dbWorkspaces = workspaceDao.findAllByFirecloudUuidIn(fcWorkspaces.keySet());

    return dbWorkspaces.stream()
        .filter(DbWorkspace::isActive)
        .map(
            dbWorkspace -> {
              String fcWorkspaceAccessLevel =
                  fcWorkspaces.get(dbWorkspace.getFirecloudUuid()).getAccessLevel();
              WorkspaceResponse currentWorkspace = new WorkspaceResponse();
              currentWorkspace.setWorkspace(manualWorkspaceMapper.toApiWorkspace(dbWorkspace));
              currentWorkspace.setAccessLevel(
                  ManualWorkspaceMapper.toApiWorkspaceAccessLevel(fcWorkspaceAccessLevel));
              return currentWorkspace;
            })
        .collect(Collectors.toList());
  }

  @Transactional
  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = getRequired(workspaceNamespace, workspaceId);

    FirecloudWorkspaceResponse fcResponse;
    FirecloudWorkspace fcWorkspace;

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
    workspaceResponse.setWorkspace(manualWorkspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));

    return workspaceResponse;
  }

  private Map<String, FirecloudWorkspaceResponse> getFirecloudWorkspaces(List<String> fields) {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    return fireCloudService.getWorkspaces(fields).stream()
        .collect(
            Collectors.toMap(
                fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                fcWorkspace -> fcWorkspace));
  }

  @Override
  public Map<String, FirecloudWorkspaceAccessEntry> getFirecloudWorkspaceAcls(
      String workspaceNamespace, String firecloudName) {
    FirecloudWorkspaceACL aclResp =
        fireCloudService.getWorkspaceAcl(workspaceNamespace, firecloudName);

    // Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
    // instead. Run this through a typed Gson conversion process to parse into the desired type.
    Type accessEntryType = new TypeToken<Map<String, FirecloudWorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  @Override
  public DbWorkspace getRequired(String ns, String firecloudName) {
    DbWorkspace workspace = get(ns, firecloudName);
    if (workspace == null) {
      throw new NotFoundException(String.format("DbWorkspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  public Optional<DbWorkspace> getByNamespace(String ns) {
    return workspaceDao.findFirstByWorkspaceNamespaceAndActiveStatusOrderByLastModifiedTimeDesc(
        ns, DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
  }

  @Override
  @Transactional
  public DbWorkspace getRequiredWithCohorts(String ns, String firecloudName) {
    DbWorkspace workspace =
        workspaceDao.findByFirecloudNameAndActiveStatusWithEagerCohorts(
            ns,
            firecloudName,
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    if (workspace == null) {
      throw new NotFoundException(String.format("DbWorkspace %s/%s not found.", ns, firecloudName));
    }
    return workspace;
  }

  @Override
  public void validateActiveBilling(String workspaceNamespace, String workspaceId)
      throws ForbiddenException {
    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      return;
    }

    if (BillingStatus.INACTIVE.equals(
        getRequired(workspaceNamespace, workspaceId).getBillingStatus())) {
      throw new ForbiddenException(
          "Workspace (" + workspaceNamespace + ") is in an inactive billing state");
    }
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
              "DbWorkspace %s/%s already %s.",
              ns, firecloudName, workspace.getApproved() ? "approved" : "rejected"));
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspace.setApproved(approved);
    saveWithLastModified(workspace, now);
  }

  @Override
  public FirecloudWorkspaceACLUpdate updateFirecloudAclsOnUser(
      WorkspaceAccessLevel updatedAccess, FirecloudWorkspaceACLUpdate currentUpdate) {
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
    Map<String, FirecloudWorkspaceAccessEntry> aclsMap =
        getFirecloudWorkspaceAcls(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());

    // Iterate through existing roles, update/remove them
    ArrayList<FirecloudWorkspaceACLUpdate> updateACLRequestList = new ArrayList<>();
    Map<String, WorkspaceAccessLevel> toAdd = new HashMap<>(updatedAclsMap);
    for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : aclsMap.entrySet()) {
      String currentUserEmail = entry.getKey();
      WorkspaceAccessLevel updatedAccess = toAdd.get(currentUserEmail);
      if (updatedAccess != null) {
        FirecloudWorkspaceACLUpdate currentUpdate = new FirecloudWorkspaceACLUpdate();
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
          FirecloudWorkspaceACLUpdate removedUser = new FirecloudWorkspaceACLUpdate();
          removedUser.setEmail(currentUserEmail);
          removedUser = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, removedUser);
          updateACLRequestList.add(removedUser);
        }
      }
    }

    // Iterate through remaining new roles; add them
    for (Entry<String, WorkspaceAccessLevel> remainingRole : toAdd.entrySet()) {
      FirecloudWorkspaceACLUpdate newUser = new FirecloudWorkspaceACLUpdate();
      newUser.setEmail(remainingRole.getKey());
      newUser = updateFirecloudAclsOnUser(remainingRole.getValue(), newUser);
      updateACLRequestList.add(newUser);
    }
    FirecloudWorkspaceACLUpdateResponseList fireCloudResponse =
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
          aclsMap
              .getOrDefault(email, new FirecloudWorkspaceAccessEntry().accessLevel(""))
              .getAccessLevel();
      WorkspaceAccessLevel toAccess =
          updatedAclsMap.getOrDefault(email, WorkspaceAccessLevel.NO_ACCESS);
      if (FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER != toAccess) {
        log.info(
            String.format(
                "removing user '%s' from billing project '%s'",
                email, workspace.getWorkspaceNamespace()));
        fireCloudService.removeOwnerFromBillingProject(
            email, workspace.getWorkspaceNamespace(), Optional.empty());
      } else if (!FC_OWNER_ROLE.equals(fromAccess) && WorkspaceAccessLevel.OWNER == toAccess) {
        log.info(
            String.format(
                "adding user '%s' to billing project '%s'",
                email, workspace.getWorkspaceNamespace()));
        fireCloudService.addOwnerToBillingProject(email, workspace.getWorkspaceNamespace());
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
    for (DbCohort fromCohort : from.getCohorts()) {
      fromCohortIdToToCohortId.put(
          fromCohort.getCohortId(),
          cohortCloningService.cloneCohortAndReviews(fromCohort, to).getCohortId());
    }
    Map<Long, Long> fromConceptSetIdToToConceptSetId = new HashMap<>();
    for (DbConceptSet fromConceptSet : conceptSetService.getConceptSets(from)) {
      fromConceptSetIdToToConceptSetId.put(
          fromConceptSet.getConceptSetId(),
          conceptSetService
              .cloneConceptSetAndConceptIds(fromConceptSet, to, cdrVersionChanged)
              .getConceptSetId());
    }
    for (DbDataset dataSet : dataSetService.getDataSets(from)) {
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
  public WorkspaceAccessLevel getWorkspaceAccessLevel(String workspaceNamespace, String workspaceId)
      throws IllegalArgumentException {
    String userAccess =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getAccessLevel();
    if (PROJECT_OWNER_ACCESS_LEVEL.equals(userAccess)) {
      return WorkspaceAccessLevel.OWNER;
    }
    return Optional.ofNullable(WorkspaceAccessLevel.fromValue(userAccess))
        .orElseThrow(
            () -> new IllegalArgumentException("Unrecognized access level: " + userAccess));
  }

  @Override
  public WorkspaceAccessLevel enforceWorkspaceAccessLevel(
      String workspaceNamespace, String workspaceId, WorkspaceAccessLevel requiredAccess) {
    final WorkspaceAccessLevel access;
    try {
      access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
    } catch (IllegalArgumentException e) {
      throw new ServerErrorException(e);
    }
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
  public Optional<DbWorkspace> findActiveByWorkspaceId(long workspaceId) {
    DbWorkspace workspace = getDao().findOne(workspaceId);
    if (workspace == null || !workspace.isActive()) {
      return Optional.empty();
    }
    return Optional.of(workspace);
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    Map<String, FirecloudWorkspaceAccessEntry> emailToRole =
        getFirecloudWorkspaceAcls(workspaceNamespace, firecloudName);

    List<UserRole> userRoles = new ArrayList<>();
    for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : emailToRole.entrySet()) {
      // Filter out groups
      DbUser user = userDao.findUserByUsername(entry.getKey());
      if (user == null) {
        log.log(Level.WARNING, "No user found for " + entry.getKey());
      } else {
        userRoles.add(manualWorkspaceMapper.toApiUserRole(user, entry.getValue()));
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
    ArrayList<FirecloudWorkspaceACLUpdate> updateACLRequestList = new ArrayList<>();
    FirecloudWorkspaceACLUpdate currentUpdate = new FirecloudWorkspaceACLUpdate();
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
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    long userId = userProvider.get().getUserId();
    List<DbUserRecentWorkspace> userRecentWorkspaces =
        userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId);
    return pruneInaccessibleRecentWorkspaces(userRecentWorkspaces, userId);
  }

  private List<DbUserRecentWorkspace> pruneInaccessibleRecentWorkspaces(
      List<DbUserRecentWorkspace> recentWorkspaces, long userId) {
    List<DbWorkspace> dbWorkspaces =
        workspaceDao.findAllByWorkspaceIdIn(
            recentWorkspaces.stream()
                .map(DbUserRecentWorkspace::getWorkspaceId)
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
  public DbUserRecentWorkspace updateRecentWorkspaces(
      DbWorkspace workspace, long userId, Timestamp lastAccessDate) {
    Optional<DbUserRecentWorkspace> maybeRecentWorkspace =
        userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspace.getWorkspaceId(), userId);
    final DbUserRecentWorkspace matchingRecentWorkspace =
        maybeRecentWorkspace
            .map(
                recentWorkspace -> {
                  recentWorkspace.setLastAccessDate(lastAccessDate);
                  return recentWorkspace;
                })
            .orElseGet(
                () ->
                    new DbUserRecentWorkspace(workspace.getWorkspaceId(), userId, lastAccessDate));
    userRecentWorkspaceDao.save(matchingRecentWorkspace);
    handleWorkspaceLimit(userId);
    return matchingRecentWorkspace;
  }

  @Override
  @Transactional
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return updateRecentWorkspaces(
        workspace, userProvider.get().getUserId(), new Timestamp(clock.instant().toEpochMilli()));
  }

  private void handleWorkspaceLimit(long userId) {
    List<DbUserRecentWorkspace> userRecentWorkspaces =
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
    Optional<DbUserRecentWorkspace> maybeRecentWorkspace =
        userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspaceId, userId);
    if (maybeRecentWorkspace.isPresent()) {
      userRecentWorkspaceDao.delete(maybeRecentWorkspace.get());
      return true;
    } else {
      return false;
    }
  }

  // this is necessary because the grant ownership call in create/clone
  // may not have propagated. Adding a few retries drastically reduces
  // the likely of failing due to slow propagation
  private Retryer<ProjectBillingInfo> cloudBillingRetryer =
      RetryerBuilder.<ProjectBillingInfo>newBuilder()
          .retryIfException(
              e ->
                  e instanceof GoogleJsonResponseException
                      && ((GoogleJsonResponseException) e).getStatusCode() == 403)
          .withWaitStrategy(WaitStrategies.exponentialWait())
          .withStopStrategy(StopStrategies.stopAfterDelay(60, TimeUnit.SECONDS))
          .build();

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {
    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout
        || newBillingAccountName.equals(workspace.getBillingAccountName())) {
      return;
    }

    Cloudbilling cloudbilling;
    if (newBillingAccountName.equals(
        workbenchConfigProvider.get().billing.freeTierBillingAccountName())) {
      cloudbilling = serviceAccountCloudbillingProvider.get();
    } else {
      cloudbilling = endUserCloudbillingProvider.get();
      try {
        if (!Optional.ofNullable(
                cloudbilling.billingAccounts().get(newBillingAccountName).execute().getOpen())
            .orElse(false)) {
          throw new BadRequestException(
              "Provided billing account is closed. Please provide an open account.");
        }
      } catch (IOException e) {
        throw new ServerErrorException("Could not fetch user provided billing account.", e);
      }
    }

    UpdateBillingInfo request;
    try {
      request =
          cloudbilling
              .projects()
              .updateBillingInfo(
                  "projects/" + workspace.getWorkspaceNamespace(),
                  new ProjectBillingInfo().setBillingAccountName(newBillingAccountName));
    } catch (IOException e) {
      throw new ServerErrorException("Could not create Google Cloud updateBillingInfo request", e);
    }

    ProjectBillingInfo response;
    try {
      response = cloudBillingRetryer.call(request::execute);
    } catch (RetryException | ExecutionException e) {
      throw new ServerErrorException("Google Cloud updateBillingInfo call failed", e);
    }

    if (!newBillingAccountName.equals(response.getBillingAccountName())) {
      throw new ServerErrorException(
          "Google Cloud updateBillingInfo call succeeded but did not set the correct billing account name");
    }

    workspace.setBillingAccountName(response.getBillingAccountName());

    if (newBillingAccountName.equals(
        workbenchConfigProvider.get().billing.freeTierBillingAccountName())) {
      workspace.setBillingStatus(
          freeTierBillingService.userHasFreeTierCredits(workspace.getCreator())
              ? BillingStatus.ACTIVE
              : BillingStatus.INACTIVE);
    } else {
      // At this point, we can assume that a user provided billing account is open since we
      // throw a BadRequestException if a closed one is provided
      workspace.setBillingStatus(BillingStatus.ACTIVE);
    }
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    final ImmutableList.Builder<MeasurementBundle> resultBuilder = ImmutableList.builder();

    // TODO(jaycarlton): fetch both active status and data access level crossed counts
    final Map<WorkspaceActiveStatus, Long> activeStatusToCount =
        workspaceDao.getActiveStatusToCountMap();
    for (WorkspaceActiveStatus status : WorkspaceActiveStatus.values()) {
      final long count = activeStatusToCount.getOrDefault(status, 0L);
      resultBuilder.add(
          MeasurementBundle.builder()
              .addMeasurement(GaugeMetric.WORKSPACE_COUNT, count)
              .addTag(MetricLabel.WORKSPACE_ACTIVE_STATUS, status.toString())
              .build());
    }
    return resultBuilder.build();
  }
}
