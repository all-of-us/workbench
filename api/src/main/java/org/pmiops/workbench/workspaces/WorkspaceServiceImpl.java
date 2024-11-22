package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.io.IOException;
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
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.pmiops.workbench.tanagra.model.Cohort;
import org.pmiops.workbench.tanagra.model.CohortCloneInfo;
import org.pmiops.workbench.tanagra.model.CohortList;
import org.pmiops.workbench.tanagra.model.FeatureSet;
import org.pmiops.workbench.tanagra.model.FeatureSetCloneInfo;
import org.pmiops.workbench.tanagra.model.FeatureSetList;
import org.pmiops.workbench.tanagra.model.StudyCreateInfo;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DbWorkspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * <p>This needs to implement an interface to support Transactional
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  private final AccessTierService accessTierService;
  private final BillingProjectAuditor billingProjectAuditor;
  private final Clock clock;
  private final CloudBillingClient cloudBillingClient;
  private final CohortCloningService cohortCloningService;
  private final ConceptSetService conceptSetService;
  private final DataSetService dataSetService;
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final FireCloudService fireCloudService;
  private final FirecloudMapper firecloudMapper;
  private final InitialCreditsService initialCreditsService;
  private final MailService mailService;
  private final Provider<DbUser> userProvider;
  private final Provider<TanagraApi> tanagraApiProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserMapper userMapper;
  private final UserRecentWorkspaceDao userRecentWorkspaceDao;
  private final UserService userService;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public WorkspaceServiceImpl(
      AccessTierService accessTierService,
      BillingProjectAuditor billingProjectAuditor,
      Clock clock,
      CloudBillingClient cloudBillingClient,
      CohortCloningService cohortCloningService,
      ConceptSetService conceptSetService,
      DataSetService dataSetService,
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      FireCloudService fireCloudService,
      FirecloudMapper firecloudMapper,
      InitialCreditsService initialCreditsService,
      MailService mailService,
      Provider<DbUser> userProvider,
      Provider<TanagraApi> tanagraApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserMapper userMapper,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      UserService userService,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.accessTierService = accessTierService;
    this.billingProjectAuditor = billingProjectAuditor;
    this.clock = clock;
    this.cloudBillingClient = cloudBillingClient;
    this.cohortCloningService = cohortCloningService;
    this.conceptSetService = conceptSetService;
    this.dataSetService = dataSetService;
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.fireCloudService = fireCloudService;
    this.firecloudMapper = firecloudMapper;
    this.initialCreditsService = initialCreditsService;
    this.mailService = mailService;
    this.tanagraApiProvider = tanagraApiProvider;
    this.userDao = userDao;
    this.userMapper = userMapper;
    this.userProvider = userProvider;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    return workspaceMapper
        .toApiWorkspaceResponseList(
            workspaceDao, fireCloudService.getWorkspaces(), initialCreditsService)
        .stream()
        .filter(WorkspaceServiceImpl::filterToNonPublished)
        .toList();
  }

  private static boolean filterToNonPublished(WorkspaceResponse response) {
    return response.getAccessLevel() == WorkspaceAccessLevel.OWNER
        || response.getAccessLevel() == WorkspaceAccessLevel.WRITER
        || response.getWorkspace().getFeaturedCategory() == null;
  }

  @Override
  public List<WorkspaceResponse> getFeaturedWorkspaces() {
    return workspaceMapper
        .toApiWorkspaceResponseList(
            workspaceDao, fireCloudService.getWorkspaces(), initialCreditsService)
        .stream()
        .filter(workspaceResponse -> workspaceResponse.getWorkspace().getFeaturedCategory() != null)
        .toList();
  }

  @Override
  public String getPublishedWorkspacesGroupEmail() {
    // All users with CT access also have RT access, so we know that any user with access to
    // workspaces will be a member of the RT Auth Domain Group.  Therefore, we can use this group
    // to assign access to all relevant users at once.
    //
    // We implement the "Publishing" of workspaces by assigning READER access to this group.
    //
    // Controlled Tier note: our intention for RT-only users is that they have -*awareness of*- but
    // not -*access to*- Published workspaces in the CT.  Our UI special-cases Published workspaces
    // to make this possible, and any user attempting to gain access to these will find that they
    // are blocked.  Despite having nominal "READER" access, their level is actually "NO ACCESS"
    // specifically because they are not members of the Controlled Tier Auth Domain.
    return accessTierService.getRegisteredTierOrThrow().getAuthDomainGroupEmail();
  }

  @Transactional
  @Override
  public WorkspaceResponse getWorkspace(String workspaceNamespace, String workspaceTerraName) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);
    validateWorkspaceTierAccess(dbWorkspace);

    RawlsWorkspaceResponse fcResponse;
    RawlsWorkspaceDetails fcWorkspace;
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();

    // This enforces access controls.
    fcResponse =
        fireCloudService.getWorkspace(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    fcWorkspace = fcResponse.getWorkspace();

    workspaceResponse.setAccessLevel(
        firecloudMapper.fcToApiWorkspaceAccessLevel(fcResponse.getAccessLevel()));
    Workspace workspace =
        workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace, initialCreditsService);
    workspaceResponse.setWorkspace(workspace);

    return workspaceResponse;
  }

  @Transactional
  @Override
  public void deleteWorkspace(DbWorkspace dbWorkspace) {
    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    // This automatically handles access control to the workspace.
    fireCloudService.deleteWorkspace(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    dbWorkspace =
        workspaceDao.saveWithLastModified(
            dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED),
            userProvider.get());
    // Since deleted workspace entry still exist in database we have to explicitly remove it from
    // featured_workspace
    // if they exist
    featuredWorkspaceDao.deleteDbFeaturedWorkspaceByWorkspace(dbWorkspace);

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
  @Transactional
  public DbWorkspace saveAndCloneCohortsConceptSetsAndDataSets(DbWorkspace from, DbWorkspace to) {
    // Save the workspace first to allocate an ID.
    to = workspaceDao.save(to);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(to.getCdrVersion());

    if (to.isCDRAndWorkspaceTanagraEnabled()) {
      // Create a new tanagra study that matches AoU workspace
      createTanagraStudy(to.getWorkspaceNamespace(), to.getName());
      // Clone tanagra cohorts
      cloneTanagraCohorts(from.getWorkspaceNamespace(), to.getWorkspaceNamespace());
      // Cone tanagra feature sets
      cloneTanagraFeatureSets(from.getWorkspaceNamespace(), to.getWorkspaceNamespace());
    } else {
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
    }
    return to;
  }

  private void cloneTanagraCohorts(String fromWorkspaceNamespace, String toWorkspaceNamespace) {
    boolean hasMoreResults = true;
    int offset = 0;
    int limit = 50;
    while (hasMoreResults) {
      List<Cohort> cohorts =
          listTanagraCohorts(fromWorkspaceNamespace, offset, limit).stream().toList();
      cohorts.forEach(
          cohort -> cloneTanagraCohort(cohort, fromWorkspaceNamespace, toWorkspaceNamespace));
      if (cohorts.size() < limit) {
        hasMoreResults = false;
      } else {
        offset += limit;
      }
    }
  }

  private void cloneTanagraFeatureSets(String fromWorkspaceNamespace, String toWorkspaceNamespace) {
    boolean hasMoreResults = true;
    int offset = 0;
    int limit = 50;
    while (hasMoreResults) {
      List<FeatureSet> featureSets =
          listTanagraFeatureSets(fromWorkspaceNamespace, offset, limit).stream().toList();
      featureSets.forEach(
          fs -> cloneTanagraFeatureSet(fs, fromWorkspaceNamespace, toWorkspaceNamespace));
      if (featureSets.size() < limit) {
        hasMoreResults = false;
      } else {
        offset += limit;
      }
    }
  }

  @Override
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    Map<String, RawlsWorkspaceAccessEntry> emailToRole =
        workspaceAuthService.getFirecloudWorkspaceAcl(workspaceNamespace, firecloudName);

    List<UserRole> userRoles = new ArrayList<>();
    for (Map.Entry<String, RawlsWorkspaceAccessEntry> entry : emailToRole.entrySet()) {
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
  @Transactional
  public List<DbUserRecentWorkspace> getRecentWorkspaces() {
    long userId = userProvider.get().getUserId();
    List<DbUserRecentWorkspace> userRecentWorkspaces =
        userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId);
    return pruneInaccessibleRecentWorkspaces(userRecentWorkspaces, userId);
  }

  /**
   * Throw ForbiddenException if logged in user doesnt have the same Tier Access as that of
   * workspace
   *
   * @param dbWorkspace
   */
  private void validateWorkspaceTierAccess(DbWorkspace dbWorkspace) {
    String workspaceAccessTier = dbWorkspace.getCdrVersion().getAccessTier().getShortName();

    List<String> accessTiers = accessTierService.getAccessTierShortNamesForUser(userProvider.get());

    if (!accessTiers.contains(workspaceAccessTier)) {
      throw new ForbiddenException(
          String.format(
              "User with username %s does not have access to the '%s' access tier required by "
                  + "workspace '%s'",
              userProvider.get().getUsername(), workspaceAccessTier, dbWorkspace.getName()));
    }
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
      /* The current table which stores user recent resources is unable to delete the entries for
        deleted Workspace.https://precisionmedicineinitiative.atlassian.net/browse/RW-6159
        The below statement does delete entries for inactive workspaces for the new table
        (user_Recent_modified_resources), however we will uncomment it only when the new
        table replaces the old version of user recent resource completely to avoid any discrepancies
      */
      //      userRecentlyModifiedResourceDao.deleteByUserIdAndWorkspaceIdIn(userId,
      // workspaceIdsToDelete);
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

  @Override
  public Map<String, DbWorkspace> getWorkspacesByGoogleProject(Set<String> googleProjectIds) {
    List<DbWorkspace> workspaces = workspaceDao.findAllByGoogleProjectIn(googleProjectIds);
    return workspaces.stream()
        .collect(Collectors.toMap(DbWorkspace::getGoogleProject, Function.identity()));
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

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {
    if (newBillingAccountName.equals(workspace.getBillingAccountName())) {
      log.info(
          String.format(
              "Billing account for workspace %s is already set to %s",
              workspace.getWorkspaceNamespace(), newBillingAccountName));
      return;
    }

    log.info(
        String.format(
            "Updating billing account name for workspace: %s. Old billing account: %s, New billing account: %s",
            workspace.getWorkspaceNamespace(),
            workspace.getBillingAccountName(),
            newBillingAccountName));

    if (isInitialCredits(newBillingAccountName, workbenchConfigProvider.get())) {
      fireCloudService.updateBillingAccountAsService(
          workspace.getWorkspaceNamespace(), newBillingAccountName);
    } else {
      fireCloudService.updateBillingAccount(
          workspace.getWorkspaceNamespace(), newBillingAccountName);
    }

    try {
      ProjectBillingInfo projectBillingInfo =
          cloudBillingClient.pollUntilBillingAccountLinked(
              workspace.getGoogleProject(), newBillingAccountName);
      if (!projectBillingInfo.getBillingEnabled()) {
        throw new FailedPreconditionException(
            String.format(
                "Provided billing account %s is closed. Please provide an open account.",
                newBillingAccountName));
      }
    } catch (IOException | InterruptedException e) {
      throw new ServerErrorException("Timed out while verifying billing account update.", e);
    }

    workspace.setBillingAccountName(newBillingAccountName);

    log.info(
        String.format(
            "Billing account name of workspace %s set to %s.",
            workspace.getWorkspaceNamespace(), newBillingAccountName));

    if (isInitialCredits(newBillingAccountName, workbenchConfigProvider.get())) {
      DbUser creator = workspace.getCreator();
      boolean hasInitialCreditsRemaining =
          initialCreditsService.userHasRemainingFreeTierCredits(creator);
      workspace.setBillingStatus(
          hasInitialCreditsRemaining ? BillingStatus.ACTIVE : BillingStatus.INACTIVE);
      workspace.setInitialCreditsExhausted(!hasInitialCreditsRemaining);
      workspace.setInitialCreditsExpired(initialCreditsService.areUserCreditsExpired(creator));
      log.info(
          String.format(
              "Initial credits status of workspace %s was updated.",
              workspace.getWorkspaceNamespace()));
    } else {
      // At this point, we can assume that a user provided billing account is open since we
      // throw a BadRequestException if a closed one is provided
      workspace.setBillingStatus(BillingStatus.ACTIVE);
      log.info(
          String.format(
              "Billing status of workspace %s set to ACTIVE.", workspace.getWorkspaceNamespace()));
    }
  }

  @Override
  public boolean notebookTransferComplete(String workspaceNamespace, String workspaceTerraName) {
    return fireCloudService.workspaceFileTransferComplete(workspaceNamespace, workspaceTerraName);
  }

  @Override
  public DbWorkspace lookupWorkspaceByNamespace(String workspaceNamespace)
      throws NotFoundException {
    return workspaceDao
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }

  @Override
  public void createTanagraStudy(String workspaceNamespace, String workspaceName) {
    try {
      StudyCreateInfo studyCreateInfo =
          new StudyCreateInfo().id(workspaceNamespace).displayName(workspaceName);
      tanagraApiProvider.get().createStudy(studyCreateInfo);
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format(
              "Could not create a Tanagra study for workspace namespace: %s, name: %s",
              workspaceNamespace, workspaceName),
          e);
    }
  }

  @Override
  public CohortList listTanagraCohorts(String workspaceNamespace, Integer offset, Integer limit) {
    try {
      return tanagraApiProvider.get().listCohorts(workspaceNamespace, offset, limit);
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format("Could not list cohorts for workspace: %s", workspaceNamespace),
          e);
    }
    return new CohortList();
  }

  @Override
  public FeatureSetList listTanagraFeatureSets(
      String workspaceNamespace, Integer offset, Integer limit) {
    try {
      return tanagraApiProvider.get().listFeatureSets(workspaceNamespace, offset, limit);
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format("Could not list feature sets for workspace: %s", workspaceNamespace),
          e);
    }
    return new FeatureSetList();
  }

  @Override
  public void cloneTanagraCohort(
      Cohort cohort, String fromWorkspaceNamespace, String toWorkspaceNamespace) {
    CohortCloneInfo cohortCloneInfo =
        new CohortCloneInfo()
            .destinationStudyId(toWorkspaceNamespace)
            .displayName(cohort.getDisplayName())
            .description(cohort.getDescription());
    try {
      tanagraApiProvider.get().cloneCohort(cohortCloneInfo, fromWorkspaceNamespace, cohort.getId());
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format("Could not clone cohort for workspace: %s", toWorkspaceNamespace),
          e);
    }
  }

  @Override
  public void cloneTanagraFeatureSet(
      FeatureSet featureSet, String fromWorkspaceNamespace, String toWorkspaceNamespace) {
    FeatureSetCloneInfo cloneInfo =
        new FeatureSetCloneInfo()
            .destinationStudyId(toWorkspaceNamespace)
            .displayName(featureSet.getDisplayName())
            .description(featureSet.getDescription());
    try {
      tanagraApiProvider
          .get()
          .cloneFeatureSet(cloneInfo, fromWorkspaceNamespace, featureSet.getId());
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format("Could not clone feature set for workspace: %s", toWorkspaceNamespace),
          e);
    }
  }

  @Override
  public void updateInitialCreditsExhaustion(DbUser user, boolean exhausted) {
    workspaceDao.findAllByCreator(user).stream()
        .filter(ws -> isInitialCredits(ws.getBillingAccountName(), workbenchConfigProvider.get()))
        .forEach(
            ws -> {
              ws.setInitialCreditsExhausted(exhausted);
              ws.setBillingStatus(exhausted ? BillingStatus.INACTIVE : BillingStatus.ACTIVE);
              workspaceDao.save(ws);
            });
  }

  @Override
  public void publishCommunityWorkspace(DbWorkspace dbWorkspace) {
    featuredWorkspaceDao
        .findByWorkspace(dbWorkspace)
        .ifPresentOrElse(
            dbFeaturedWorkspace -> {
              // Throw exception if workspace is already Published
              throw new BadRequestException("Workspace is already published");
            },
            () -> {
              fireCloudService.updateWorkspaceAclForPublishing(
                  dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName(), true);

              DbFeaturedWorkspace dbFeaturedWorkspaceToSave =
                  featuredWorkspaceMapper.toDbFeaturedWorkspace(
                      FeaturedWorkspaceCategory.COMMUNITY, dbWorkspace);
              featuredWorkspaceDao.save(dbFeaturedWorkspaceToSave);

              try {
                mailService.sendPublishCommunityWorkspaceEmails(
                    dbWorkspace, getWorkspaceOwnerList(dbWorkspace));
              } catch (MessagingException e) {
                log.log(Level.WARNING, e.getMessage());
              }
            });
  }

  @Override
  public List<DbUser> getWorkspaceOwnerList(DbWorkspace dbWorkspace) {
    return getFirecloudUserRoles(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
        .stream()
        .filter(userRole -> userRole.getRole() == WorkspaceAccessLevel.OWNER)
        .map(UserRole::getEmail)
        .map(userService::getByUsernameOrThrow)
        .toList();
  }
}
