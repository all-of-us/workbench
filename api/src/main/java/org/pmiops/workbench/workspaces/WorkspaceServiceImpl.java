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
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao.ActiveStatusToCountResult;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());
  private static final String FC_OWNER_ROLE = "OWNER";

  private final BillingProjectAuditor billingProjectAuditor;
  private final Clock clock;
  private final CohortCloningService cohortCloningService;
  private final ConceptSetService conceptSetService;
  private final DataSetService dataSetService;
  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final Provider<Cloudbilling> endUserCloudbillingProvider;
  private final Provider<Cloudbilling> serviceAccountCloudbillingProvider;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserMapper userMapper;
  private final UserRecentWorkspaceDao userRecentWorkspaceDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceAuthService workspaceAuthService;

  @Autowired
  public WorkspaceServiceImpl(
      @Qualifier(END_USER_CLOUD_BILLING) Provider<Cloudbilling> endUserCloudbillingProvider,
      @Qualifier(SERVICE_ACCOUNT_CLOUD_BILLING)
          Provider<Cloudbilling> serviceAccountCloudbillingProvider,
      BillingProjectAuditor billingProjectAuditor,
      Clock clock,
      CohortCloningService cohortCloningService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserMapper userMapper,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceAuthService workspaceAuthService) {
    this.endUserCloudbillingProvider = endUserCloudbillingProvider;
    this.serviceAccountCloudbillingProvider = serviceAccountCloudbillingProvider;
    this.billingProjectAuditor = billingProjectAuditor;
    this.clock = clock;
    this.cohortCloningService = cohortCloningService;
    this.conceptSetService = conceptSetService;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.userDao = userDao;
    this.userMapper = userMapper;
    this.userProvider = userProvider;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceAuthService = workspaceAuthService;
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

  private List<WorkspaceResponse> getWorkspacesAndPublicWorkspaces() {
    Map<String, FirecloudWorkspaceResponse> fcWorkspacesByUuid = getFirecloudWorkspaces();
    List<DbWorkspace> dbWorkspaces =
        workspaceDao.findAllByFirecloudUuidIn(fcWorkspacesByUuid.keySet());
    return dbWorkspaces.stream()
        .filter(DbWorkspace::isActive)
        .map(
            dbWorkspace ->
                workspaceMapper.toApiWorkspaceResponse(
                    dbWorkspace, fcWorkspacesByUuid.get(dbWorkspace.getFirecloudUuid())))
        .collect(Collectors.toList());
  }

  private Map<String, FirecloudWorkspaceResponse> getFirecloudWorkspaces() {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    return fireCloudService.getWorkspaces().stream()
        .collect(
            Collectors.toMap(
                fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                fcWorkspace -> fcWorkspace));
  }

  @Transactional
  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    FirecloudWorkspaceResponse fcResponse;
    FirecloudWorkspace fcWorkspace;

    WorkspaceResponse workspaceResponse = new WorkspaceResponse();

    // This enforces access controls.
    fcResponse =
        fireCloudService.getWorkspace(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    fcWorkspace = fcResponse.getWorkspace();

    if (fcResponse.getAccessLevel().equals(WorkspaceAuthService.PROJECT_OWNER_ACCESS_LEVEL)) {
      // We don't expose PROJECT_OWNER in our API; just use OWNER.
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.OWNER);
    } else {
      workspaceResponse.setAccessLevel(WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel()));
      if (workspaceResponse.getAccessLevel() == null) {
        throw new ServerErrorException("Unsupported access level: " + fcResponse.getAccessLevel());
      }
    }
    workspaceResponse.setWorkspace(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));

    return workspaceResponse;
  }

  @Override
  public void deleteWorkspace(DbWorkspace dbWorkspace) {
    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    // This automatically handles access control to the workspace.
    fireCloudService.deleteWorkspace(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace);
    userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(
        userProvider.get().getUserId(), Collections.singletonList(dbWorkspace.getWorkspaceId()));

    String billingProjectName = dbWorkspace.getWorkspaceNamespace();
    try {
      fireCloudService.deleteBillingProject(billingProjectName);
      billingProjectAuditor.fireDeleteAction(billingProjectName);
    } catch (Exception e) {
      String msg =
          String.format(
              "Error deleting billing project %s: %s", billingProjectName, e.getMessage());
      log.warning(msg);
    }
  }

  @Override
  public void setResearchPurposeApproved(String ns, String firecloudName, boolean approved) {
    DbWorkspace workspace = workspaceDao.getRequired(ns, firecloudName);
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
    workspace.setApproved(approved);
    workspaceDao.saveWithLastModified(workspace);
  }

  @Override
  @Transactional
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    // Save the workspace first to allocate an ID.
    to = workspaceDao.save(to);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(to.getCdrVersion());
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
          conceptSetService.cloneConceptSetAndConceptIds(fromConceptSet, to).getConceptSetId());
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
              .collect(Collectors.toSet()),
          new ArrayList<>(dataSet.getPrePackagedConceptSet()));
    }
    return to;
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    Map<String, FirecloudWorkspaceAccessEntry> emailToRole =
        workspaceAuthService.getFirecloudWorkspaceAcls(workspaceNamespace, firecloudName);

    List<UserRole> userRoles = new ArrayList<>();
    for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : emailToRole.entrySet()) {
      // Filter out groups
      DbUser user = userDao.findUserByUsername(entry.getKey());
      if (user == null) {
        log.log(Level.WARNING, "No user found for " + entry.getKey());
      } else {
        userRoles.add(userMapper.toApiUserRole(user, entry.getValue()));
      }
    }
    return userRoles.stream()
        .sorted(
            Comparator.comparing(UserRole::getRole).thenComparing(UserRole::getEmail).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public DbWorkspace setPublished(
      String workspaceNamespace, String firecloudName, boolean publish) {
    final DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, firecloudName);

    final WorkspaceAccessLevel accessLevel =
        publish ? WorkspaceAccessLevel.READER : WorkspaceAccessLevel.NO_ACCESS;

    final FirecloudManagedGroupWithMembers authDomainGroup =
        fireCloudService.getGroup(dbWorkspace.getCdrVersion().getAccessTier().getAuthDomainName());

    final FirecloudWorkspaceACLUpdate currentUpdate =
        WorkspaceAuthService.updateFirecloudAclsOnUser(
            accessLevel, new FirecloudWorkspaceACLUpdate().email(authDomainGroup.getGroupEmail()));

    fireCloudService.updateWorkspaceACL(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        Collections.singletonList(currentUpdate));

    dbWorkspace.setPublished(publish);
    return workspaceDao.saveWithLastModified(dbWorkspace);
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
                    workspaceAuthService.enforceWorkspaceAccessLevel(
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
  @Transactional
  public DbUserRecentWorkspace updateRecentWorkspaces(DbWorkspace workspace) {
    return updateRecentWorkspaces(
        workspace, userProvider.get().getUserId(), new Timestamp(clock.instant().toEpochMilli()));
  }

  private DbUserRecentWorkspace updateRecentWorkspaces(
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
    if (!workbenchConfigProvider.get().featureFlags.enableBillingUpgrade
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
        Optional<Boolean> isOpenMaybe =
            Optional.ofNullable(
                cloudbilling.billingAccounts().get(newBillingAccountName).execute().getOpen());
        boolean isOpen = isOpenMaybe.orElse(false);
        if (!isOpen) {
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
                  "projects/" + workspace.getGoogleProject(),
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
          freeTierBillingService.userHasRemainingFreeTierCredits(workspace.getCreator())
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
    final List<ActiveStatusToCountResult> rows = workspaceDao.getActiveStatusToCount();
    return rows.stream()
        .map(
            row ->
                MeasurementBundle.builder()
                    .addTag(
                        MetricLabel.WORKSPACE_ACTIVE_STATUS,
                        DbStorageEnums.workspaceActiveStatusFromStorage(
                                row.getWorkspaceActiveStatus())
                            .toString())

                    // tmp record all workspaces as Registered Tier.
                    // This is mostly true in test/local and fully true in higher environments.
                    // RW-6137: Replace with AccessTier
                    .addTag(MetricLabel.DATA_ACCESS_LEVEL, DataAccessLevel.REGISTERED.toString())
                    .addMeasurement(GaugeMetric.WORKSPACE_COUNT, row.getWorkspaceCount())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }
}
