package org.pmiops.workbench.workspaces;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;
import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DbWorkspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 * <p>This needs to implement an interface to support Transactional
 */
@Service
@Primary
public class WorkspaceServiceImpl implements WorkspaceService {

  protected static final int RECENT_WORKSPACE_COUNT = 4;
  private static final Logger log = Logger.getLogger(WorkspaceServiceImpl.class.getName());

  private final AccessTierService accessTierService;
  private final ActionAuditQueryService actionAuditQueryService;
  private final BillingProjectAuditor billingProjectAuditor;
  private final Clock clock;
  private final CloudBillingClient cloudBillingClient;
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final FireCloudService fireCloudService;
  private final FirecloudMapper firecloudMapper;
  private final InitialCreditsService initialCreditsService;
  private final MailService mailService;
  private final Provider<DbUser> userProvider;
  private final Provider<Stopwatch> stopwatchProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserMapper userMapper;
  private final UserRecentWorkspaceDao userRecentWorkspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public WorkspaceServiceImpl(
      AccessTierService accessTierService,
      ActionAuditQueryService actionAuditQueryService,
      BillingProjectAuditor billingProjectAuditor,
      Clock clock,
      CloudBillingClient cloudBillingClient,
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      FireCloudService fireCloudService,
      FirecloudMapper firecloudMapper,
      InitialCreditsService initialCreditsService,
      MailService mailService,
      Provider<DbUser> userProvider,
      Provider<Stopwatch> stopwatchProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserMapper userMapper,
      UserRecentWorkspaceDao userRecentWorkspaceDao,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.accessTierService = accessTierService;
    this.actionAuditQueryService = actionAuditQueryService;
    this.billingProjectAuditor = billingProjectAuditor;
    this.clock = clock;
    this.cloudBillingClient = cloudBillingClient;
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.fireCloudService = fireCloudService;
    this.firecloudMapper = firecloudMapper;
    this.initialCreditsService = initialCreditsService;
    this.mailService = mailService;
    this.stopwatchProvider = stopwatchProvider;
    this.userDao = userDao;
    this.userMapper = userMapper;
    this.userProvider = userProvider;
    this.userRecentWorkspaceDao = userRecentWorkspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<WorkspaceResponse> listWorkspaces() {
    // Get ids and roles for all workspaces that have been shared with the current user
    List<ActionAuditQueryService.WorkspaceIdWithRoleImpl> collaboratorWorkspaceIdsWithRole =
        actionAuditQueryService.getWorkspaceIdsAndRolesByCollaboratorId(
            userProvider.get().getUserId());

    List<Long> creatorWorkspaceIds =
        workspaceDao
            .findAllWorkspaceIdsByCreatorAndActiveStatus(
                userProvider.get(),
                DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED))
            .stream()
            .toList();
    List<ActionAuditQueryService.WorkspaceIdWithRoleImpl> allWorkspaceIdsWithRole =
        new ArrayList<>();
    creatorWorkspaceIds.forEach(
        workspaceId -> {
          allWorkspaceIdsWithRole.add(
              new ActionAuditQueryService.WorkspaceIdWithRoleImpl(workspaceId, "OWNER"));
        });
    allWorkspaceIdsWithRole.addAll(collaboratorWorkspaceIdsWithRole);

    List<WorkspaceResponse> allWorkspaces = new ArrayList<>();
    allWorkspaceIdsWithRole.forEach(
        workspaceIdWithRole -> {
          if (workspaceIdWithRole.workspaceId() != null) {
            DbWorkspace workspace =
                workspaceDao.findByWorkspaceIdAndActiveStatus(
                    workspaceIdWithRole.workspaceId(),
                    DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED));
            if (workspace != null && workspace.getRecoveryState() != null) {
              WorkspaceResponse response = new WorkspaceResponse();
              response.setAccessLevel(WorkspaceAccessLevel.fromValue(workspaceIdWithRole.role()));
              response.setWorkspace(
                  workspaceMapper.toApiWorkspace(workspace, null, initialCreditsService));
              allWorkspaces.add(response);
            }
          }
        });

    List<WorkspaceResponse> activeWorkspaces =
        workspaceMapper
            .toApiWorkspaceResponseList(
                workspaceDao, fireCloudService.listWorkspaces(), initialCreditsService)
            .stream()
            .filter(WorkspaceServiceImpl::filterToNonPublished)
            .toList();
    allWorkspaces.addAll(activeWorkspaces);
    return allWorkspaces;
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
            workspaceDao, fireCloudService.listWorkspaces(), initialCreditsService)
        .stream()
        .filter(workspaceResponse -> workspaceResponse.getWorkspace().getFeaturedCategory() != null)
        .toList();
  }

  @Override
  public List<String> getActiveWorkspaceNamespacesAsService() {
    // temp performance logging
    Stopwatch stopwatch = stopwatchProvider.get().start();
    List<RawlsWorkspaceListResponse> terraWorkspaces = fireCloudService.listWorkspacesAsService();
    Duration elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "getActiveWorkspaceNamespacesAsService: Retrieved %d Terra workspaces in %s",
            terraWorkspaces.size(), formatDurationPretty(elapsed)));

    List<String> terraWorkspaceIds =
        terraWorkspaces.stream()
            .map(RawlsWorkspaceListResponse::getWorkspace)
            .map(RawlsWorkspaceDetails::getWorkspaceId)
            .toList();

    stopwatch.reset().start();
    List<String> rwbNamespaces =
        workspaceDao.findNamespacesByActiveStatusAndFirecloudUuidIn(terraWorkspaceIds);
    elapsed = stopwatch.stop().elapsed();
    log.info(
        String.format(
            "getActiveWorkspaceNamespacesAsService: Retrieved %d RWB workspaces from DB in %s",
            rwbNamespaces.size(), formatDurationPretty(elapsed)));

    return rwbNamespaces;
  }

  @Override
  public List<String> getOrphanedWorkspaceNamespacesAsService() {
    List<String> activeWorkspaceNamespaces = getActiveWorkspaceNamespacesAsService();
    return workspaceDao.findAllOrphanedWorkspaceNamespaces(activeWorkspaceNamespaces);
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
    workspaceAuthService.validateWorkspaceTierAccess(dbWorkspace);

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

  @Transactional
  @Override
  public void deleteWorkspaceAsService(DbWorkspace dbWorkspace) {
    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?

    try {
      // This automatically handles access control to the workspace.
      fireCloudService.deleteWorkspaceAsService(
          dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        // Doesn't exist in Terra. So we don't throw an exception, just continue to delete the
        // workspace in RWB
        log.warning(
            String.format(
                "Workspace does not exist in Terra. Continue deleting from RW: %s/%s: %s",
                dbWorkspace.getWorkspaceNamespace(),
                dbWorkspace.getFirecloudName(),
                e.getMessage()));
      } else {
        throw new ServerErrorException(
            String.format(
                "Error deleting workspace in Terra %s/%s: %s",
                dbWorkspace.getWorkspaceNamespace(),
                dbWorkspace.getFirecloudName(),
                e.getMessage()),
            e);
      }
    }
    dbWorkspace
        .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED)
        .setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()))
        .setLastModifiedBy(workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0));
    workspaceDao.save(dbWorkspace);
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
  public List<UserRole> getFirecloudUserRoles(String workspaceNamespace, String firecloudName) {
    Map<String, RawlsWorkspaceAccessEntry> emailToRole =
        workspaceAuthService.getFirecloudWorkspaceAcl(workspaceNamespace, firecloudName);

    var userMap = userDao.getUsersMappedByUsernames(emailToRole.keySet());

    return emailToRole.entrySet().stream()
        .flatMap(
            entry -> {
              String email = entry.getKey();
              RawlsWorkspaceAccessEntry acl = entry.getValue();
              DbUser user = userMap.get(entry.getKey());
              // Filter out groups
              if (user == null) {
                log.log(Level.WARNING, "No user found for " + email);
                return Stream.empty();
              } else {
                return Stream.of(userMapper.toApiUserRole(user, acl));
              }
            })
        .sorted(
            Comparator.comparing(UserRole::getRole).thenComparing(UserRole::getEmail).reversed())
        .toList();
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
  public void updateWorkspaceBillingAccount(
      DbWorkspace workspace, String newBillingAccountName, boolean serviceAccount) {
    if (newBillingAccountName.equals(workspace.getBillingAccountName())) {
      return;
    }

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
              workspace.getGoogleProject(), newBillingAccountName, serviceAccount);
      if (!projectBillingInfo.getBillingEnabled()) {
        throw new FailedPreconditionException(
            "Provided billing account is closed. Please provide an open account.");
      }
    } catch (IOException | InterruptedException e) {
      throw new ServerErrorException("Timed out while verifying billing account update.", e);
    }

    workspace.setBillingAccountName(newBillingAccountName);

    if (isInitialCredits(newBillingAccountName, workbenchConfigProvider.get())) {
      DbUser creator = workspace.getCreator();
      boolean hasInitialCreditsRemaining =
          initialCreditsService.userHasRemainingInitialCredits(creator);
      workspace.setInitialCreditsExhausted(!hasInitialCreditsRemaining);
    }
  }

  @Override
  public void updateWorkspaceBillingAccount(DbWorkspace workspace, String newBillingAccountName) {
    updateWorkspaceBillingAccount(workspace, newBillingAccountName, false);
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
  public List<DbWorkspace> lookupWorkspacesByNamespace(Collection<String> workspaceNamespaces) {
    return workspaceDao.getByWorkspaceNamespaceIn(workspaceNamespaces);
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
    return userDao.findUsersByUsernameIn(
        getFirecloudUserRoles(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
            .stream()
            .filter(userRole -> userRole.getRole() == WorkspaceAccessLevel.OWNER)
            .map(UserRole::getEmail)
            .toList());
  }

  @Override
  public RawlsWorkspaceDetails createWorkspace(Workspace workspace, DbCdrVersion cdrVersion) {
    DbAccessTier accessTier = cdrVersion.getAccessTier();
    String billingProject = createTerraBillingProject(accessTier);
    String firecloudName = FireCloudService.toFirecloudName(workspace.getName());

    return fireCloudService.createWorkspace(
        billingProject, firecloudName, accessTier.getAuthDomainName());
  }

  @Override
  public RawlsWorkspaceDetails cloneWorkspace(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      Workspace toWorkspace,
      DbCdrVersion cdrVersion) {
    // Note: please keep any initialization logic here in sync with createWorkspace().
    DbAccessTier accessTier = cdrVersion.getAccessTier();
    String billingProject = createTerraBillingProject(accessTier);
    String firecloudName = FireCloudService.toFirecloudName(toWorkspace.getName());

    return fireCloudService.cloneWorkspace(
        fromWorkspaceNamespace,
        fromWorkspaceId,
        billingProject,
        firecloudName,
        accessTier.getAuthDomainName());
  }

  /** Creates a Terra (FireCloud) Billing project and adds the current user as owner. */
  private String createTerraBillingProject(DbAccessTier accessTier) {
    DbUser user = userProvider.get();
    String billingProject = fireCloudService.createBillingProjectName();
    fireCloudService.createAllOfUsBillingProject(billingProject, accessTier.getServicePerimeter());

    // We use the AoU Application Service Account to create the billing account, then add the user
    // as an additional owner.  In this way, we can make sure that the AoU App SA is an owner on
    // all billing projects.
    fireCloudService.addOwnerToBillingProject(user.getUsername(), billingProject);
    return billingProject;
  }
}
