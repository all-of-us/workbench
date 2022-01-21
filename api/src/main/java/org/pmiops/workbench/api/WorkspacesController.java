package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.FirecloudWorkspaceId;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.TooManyRequestsException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.RecentWorkspaceResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceBillingUsageResponse;
import org.pmiops.workbench.model.WorkspaceCreatorFreeCreditsRemainingResponse;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.model.WorkspaceResourcesRequest;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.model.WorkspaceUserRolesResponse;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspacesController implements WorkspacesApiDelegate {

  private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

  private final CdrVersionDao cdrVersionDao;
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final WorkspaceAuditor workspaceAuditor;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceResourcesService workspaceResourcesService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceService workspaceService;
  private final WorkspaceAuthService workspaceAuthService;
  private final IamService iamService;

  @Autowired
  public WorkspacesController(
      CdrVersionDao cdrVersionDao,
      Clock clock,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      WorkspaceAuditor workspaceAuditor,
      WorkspaceMapper workspaceMapper,
      WorkspaceResourcesService workspaceResourcesService,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      WorkspaceAuthService workspaceAuthService,
      IamService iamService) {
    this.cdrVersionDao = cdrVersionDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuditor = workspaceAuditor;
    this.workspaceMapper = workspaceMapper;
    this.workspaceResourcesService = workspaceResourcesService;
    this.workspaceService = workspaceService;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.iamService = iamService;
  }

  private DbCdrVersion getLiveCdrVersionId(String cdrVersionId) {
    if (Strings.isNullOrEmpty(cdrVersionId)) {
      throw new BadRequestException("missing cdrVersionId");
    }
    try {
      DbCdrVersion cdrVersion =
          cdrVersionDao
              .findById(Long.parseLong(cdrVersionId))
              .orElseThrow(
                  () ->
                      new BadRequestException(
                          String.format("CDR version with ID %s not found", cdrVersionId)));

      if (ArchivalStatus.LIVE != cdrVersion.getArchivalStatusEnum()) {
        throw new FailedPreconditionException(
            String.format(
                "CDR version with ID %s is not live, please select a different CDR version",
                cdrVersionId));
      }
      return cdrVersion;
    } catch (NumberFormatException e) {
      throw new BadRequestException(String.format("Invalid cdr version ID: %s", cdrVersionId));
    }
  }

  private FirecloudWorkspaceId generateFirecloudWorkspaceId(String namespace, String name) {
    return new FirecloudWorkspaceId(namespace, FireCloudService.toFirecloudName(name));
  }

  @Override
  public ResponseEntity<Workspace> createWorkspace(Workspace workspace) throws BadRequestException {
    validateWorkspaceApiModel(workspace);

    DbCdrVersion cdrVersion = getLiveCdrVersionId(workspace.getCdrVersionId());
    DbAccessTier accessTier = cdrVersion.getAccessTier();
    DbUser user = userProvider.get();

    // Note: please keep any initialization logic here in sync with cloneWorkspaceImpl().
    FirecloudWorkspaceId workspaceId = getFcBillingProject(accessTier, workspace);
    FirecloudWorkspaceDetails fcWorkspace =
        fireCloudService.createWorkspace(
            workspaceId.getWorkspaceNamespace(),
            workspaceId.getWorkspaceName(),
            accessTier.getAuthDomainName());
    DbWorkspace dbWorkspace = createDbWorkspace(workspace, cdrVersion, user, fcWorkspace);
    try {
      dbWorkspace = workspaceDao.save(dbWorkspace);
    } catch (Exception e) {
      // Tell Google to set the billing account back to the free tier if the workspace
      // creation fails
      log.log(
          Level.SEVERE,
          "Could not save new workspace to database. Calling Google Cloud billing to update the failed billing project's billing account back to the free tier.",
          e);

      // I don't think this is a bug but it's confusing that we're calling a function that is
      // updating the dbWorkspace object and expecting for it to not be saved.
      // There might be a refactoring opportunity here to separate out the Google Cloud
      // API calls so we can call just that instead of this which does that and a little more.
      workspaceService.updateWorkspaceBillingAccount(
          dbWorkspace, workbenchConfigProvider.get().billing.freeTierBillingAccountName());
      throw e;
    }
    if (accessTier.getEnableUserWorkflows()) {
      iamService.grantWorkflowRunnerRoleToCurrentUser(dbWorkspace.getGoogleProject());
    }
    final Workspace createdWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);
    workspaceAuditor.fireCreateAction(createdWorkspace, dbWorkspace.getWorkspaceId());
    return ResponseEntity.ok(createdWorkspace);
  }

  private DbWorkspace createDbWorkspace(
      Workspace workspace,
      DbCdrVersion cdrVersion,
      DbUser user,
      FirecloudWorkspaceDetails fcWorkspace) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    // The final step in the process is to clone the AoU representation of the
    // workspace. The implication here is that we may generate orphaned
    // Firecloud workspaces / buckets, but a user should not be able to see
    // half-way cloned workspaces via AoU - so it will just appear as a
    // transient failure.
    DbWorkspace dbWorkspace = new DbWorkspace();

    dbWorkspace.setName(workspace.getName());
    dbWorkspace.setCreator(user);
    dbWorkspace.setFirecloudName(fcWorkspace.getName());
    dbWorkspace.setWorkspaceNamespace(fcWorkspace.getNamespace());
    dbWorkspace.setFirecloudUuid(fcWorkspace.getWorkspaceId());
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    dbWorkspace.setCdrVersion(cdrVersion);
    dbWorkspace.setGoogleProject(fcWorkspace.getGoogleProject());

    // Ignore incoming fields pertaining to review status; clients can only request a review.
    workspaceMapper.mergeResearchPurposeIntoWorkspace(dbWorkspace, workspace.getResearchPurpose());
    if (workspace.getResearchPurpose().getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(workspace.getResearchPurpose().getReviewRequested());

    // A little unintuitive but setting this here reflects the current state of the workspace
    // while it was in the billing buffer. Setting this value will inform the update billing
    // code to skip an unnecessary GCP API call if the billing account is being kept at the free
    // tier
    dbWorkspace.setBillingAccountName(
        workbenchConfigProvider.get().billing.freeTierBillingAccountName());

    try {
      workspaceService.updateWorkspaceBillingAccount(
          dbWorkspace, workspace.getBillingAccountName());
    } catch (ServerErrorException e) {
      new ServerErrorException("Could not update the workspace's billing account", e);
    }

    return dbWorkspace;
  }

  private void validateWorkspaceApiModel(Workspace workspace) {
    if (Strings.isNullOrEmpty(workspace.getName())) {
      throw new BadRequestException("missing required field 'name'");
    } else if (workspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'researchPurpose'");
    } else if (workspace.getName().length() > 80) {
      throw new BadRequestException("workspace name must be 80 characters or less");
    }
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteWorkspace(
      String workspaceNamespace, String workspaceId) {

    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    workspaceService.deleteWorkspace(dbWorkspace);
    workspaceAuditor.fireDeleteAction(dbWorkspace);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(workspaceService.getWorkspace(workspaceNamespace, workspaceId));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    return ResponseEntity.ok(
        new WorkspaceResponseListResponse().items(workspaceService.getWorkspaces()));
  }

  @Override
  public ResponseEntity<Boolean> notebookTransferComplete(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(
        workspaceService.notebookTransferComplete(workspaceNamespace, workspaceId));
  }

  @Override
  public ResponseEntity<Workspace> updateWorkspace(
      String workspaceNamespace, String workspaceId, UpdateWorkspaceRequest request)
      throws NotFoundException {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER);
    Workspace workspace = request.getWorkspace();
    FirecloudWorkspaceDetails fcWorkspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getWorkspace();
    if (workspace == null) {
      throw new BadRequestException("No workspace provided in request");
    }
    if (Strings.isNullOrEmpty(workspace.getEtag())) {
      throw new BadRequestException("Missing required update field 'etag'");
    }

    final Workspace originalWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);

    int version = Etags.toVersion(workspace.getEtag());
    if (dbWorkspace.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated workspace version");
    }
    if (!dbWorkspace
        .getCdrVersion()
        .getAccessTier()
        .getShortName()
        .equals(workspace.getAccessTierShortName())) {
      throw new BadRequestException("Attempted to change data access tier");
    }
    if (workspace.getName() != null) {
      dbWorkspace.setName(workspace.getName());
    }
    ResearchPurpose researchPurpose = request.getWorkspace().getResearchPurpose();
    if (researchPurpose != null) {
      workspaceMapper.mergeResearchPurposeIntoWorkspace(dbWorkspace, researchPurpose);
      dbWorkspace.setReviewRequested(researchPurpose.getReviewRequested());
      if (researchPurpose.getReviewRequested()) {
        Timestamp now = new Timestamp(clock.instant().toEpochMilli());
        dbWorkspace.setTimeRequested(now);
      }
    }

    if (workspace.getBillingAccountName() != null) {
      try {
        workspaceService.updateWorkspaceBillingAccount(
            dbWorkspace, request.getWorkspace().getBillingAccountName());
      } catch (ServerErrorException e) {
        new ServerErrorException("Could not update the workspace's billing account", e);
      }
    }

    try {
      // The version asserted on save is the same as the one we read via
      // getRequired() above, see RW-215 for details.
      dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace);
    } catch (Exception e) {
      // Tell Google Cloud to set the billing account back to the original one since our
      // update database call failed
      workspaceService.updateWorkspaceBillingAccount(
          dbWorkspace, originalWorkspace.getBillingAccountName());
      throw e;
    }

    final Workspace editedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);

    workspaceAuditor.fireEditAction(
        originalWorkspace, editedWorkspace, dbWorkspace.getWorkspaceId());
    return ResponseEntity.ok(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
  }

  @Override
  public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(
      String fromWorkspaceNamespace, String fromWorkspaceId, CloneWorkspaceRequest body)
      throws BadRequestException, TooManyRequestsException {
    Workspace toWorkspace = body.getWorkspace();
    validateWorkspaceApiModel(toWorkspace);

    // First verify the caller has read access to the source workspace.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);

    DbWorkspace fromWorkspace =
        workspaceDao.getRequiredWithCohorts(fromWorkspaceNamespace, fromWorkspaceId);
    if (fromWorkspace == null) {
      throw new NotFoundException(
          String.format("DbWorkspace %s/%s not found", fromWorkspaceNamespace, fromWorkspaceId));
    }

    DbAccessTier accessTier = fromWorkspace.getCdrVersion().getAccessTier();

    // When specifying a CDR Version in the request, it must be live and
    // in the same Access Tier as the source Workspace's CDR Version.
    //
    // If the request is lacking a CDR Version, use the source's.

    final DbCdrVersion toCdrVersion;
    String reqCdrVersionId = body.getWorkspace().getCdrVersionId();
    if (Strings.isNullOrEmpty(reqCdrVersionId)
        || Long.parseLong(reqCdrVersionId) == fromWorkspace.getCdrVersion().getCdrVersionId()) {
      toCdrVersion = fromWorkspace.getCdrVersion();
    } else {
      toCdrVersion = getLiveCdrVersionId(reqCdrVersionId);

      if (!toCdrVersion.getAccessTier().equals(accessTier)) {
        throw new BadRequestException(
            String.format(
                "Destination workspace Access Tier '%s' does not match source workspace Access Tier '%s'",
                toCdrVersion.getAccessTier().getShortName(), accessTier.getShortName()));
      }
    }

    DbUser user = userProvider.get();
    // Note: please keep any initialization logic here in sync with createWorkspaceImpl().
    FirecloudWorkspaceId toFcWorkspaceId = getFcBillingProject(accessTier, toWorkspace);
    FirecloudWorkspaceDetails toFcWorkspace =
        fireCloudService.cloneWorkspace(
            fromWorkspaceNamespace,
            fromWorkspaceId,
            toFcWorkspaceId.getWorkspaceNamespace(),
            toFcWorkspaceId.getWorkspaceName(),
            accessTier.getAuthDomainName());
    DbWorkspace dbWorkspace = createDbWorkspace(toWorkspace, toCdrVersion, user, toFcWorkspace);
    try {
      dbWorkspace =
          workspaceService.saveAndCloneCohortsConceptSetsAndDataSets(fromWorkspace, dbWorkspace);
    } catch (Exception e) {
      // Tell Google to set the billing account back to the free tier if our clone fails
      workspaceService.updateWorkspaceBillingAccount(
          dbWorkspace, workbenchConfigProvider.get().billing.freeTierBillingAccountName());
      throw e;
    }

    // Note: It is possible for a workspace to be (partially) created and return
    // a 500 to the user if this block of code fails since the workspace is already
    // committed to the database in an earlier call
    Map<String, WorkspaceAccessLevel> clonedRoles = new HashMap<>();
    if (Optional.ofNullable(body.getIncludeUserRoles()).orElse(false)) {
      Map<String, FirecloudWorkspaceAccessEntry> fromAclsMap =
          workspaceAuthService.getFirecloudWorkspaceAcls(
              fromWorkspace.getWorkspaceNamespace(), fromWorkspace.getFirecloudName());
      for (Map.Entry<String, FirecloudWorkspaceAccessEntry> entry : fromAclsMap.entrySet()) {
        if (!entry.getKey().equals(user.getUsername())) {
          clonedRoles.put(
              entry.getKey(), WorkspaceAccessLevel.fromValue(entry.getValue().getAccessLevel()));
        } else {
          clonedRoles.put(entry.getKey(), WorkspaceAccessLevel.OWNER);
        }
      }
      dbWorkspace = workspaceAuthService.updateWorkspaceAcls(dbWorkspace, clonedRoles);
    }

    dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace);

    // Grant the workspace cloner and all from-workspaces users permission to use workflow if
    // workspace is controlled tier workspace.
    if (accessTier.getEnableUserWorkflows()) {
      iamService.grantWorkflowRunnerRoleToCurrentUser(dbWorkspace.getGoogleProject());
      List<String> usersGainPermission =
          clonedRoles.entrySet().stream()
              .filter(entry -> shouldGrantWorkflowRunnerAsService(user, entry))
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());
      iamService.grantWorkflowRunnerRoleToUsers(
          dbWorkspace.getGoogleProject(), usersGainPermission);
    }
    final Workspace savedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, toFcWorkspace);
    workspaceAuditor.fireDuplicateAction(
        fromWorkspace.getWorkspaceId(), dbWorkspace.getWorkspaceId(), savedWorkspace);
    return ResponseEntity.ok(new CloneWorkspaceResponse().workspace(savedWorkspace));
  }

  /** Gets a FireCloud Billing project. */
  private FirecloudWorkspaceId getFcBillingProject(DbAccessTier accessTier, Workspace workspace) {
    DbUser user = userProvider.get();
    String billingProject = fireCloudService.createBillingProjectName();
    fireCloudService.createAllOfUsBillingProject(billingProject, accessTier.getServicePerimeter());

    // We use AoU Service Account to create the billing account then assign owner role to user.
    // In this way, we can make sure AoU Service Account is still the owner of this billing
    // account.
    fireCloudService.addOwnerToBillingProject(user.getUsername(), billingProject);
    return generateFirecloudWorkspaceId(billingProject, workspace.getName());
  }

  @Override
  public ResponseEntity<WorkspaceBillingUsageResponse> getBillingUsage(
      String workspaceNamespace, String workspaceId) {
    // This is its own method as opposed to part of the workspace response because this is gated
    // behind write+ access, and adding access based composition to the workspace response
    // would add a lot of unnecessary complexity.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    return ResponseEntity.ok(
        new WorkspaceBillingUsageResponse()
            .cost(freeTierBillingService.getWorkspaceFreeTierBillingUsage(workspace)));
  }

  @Override
  public ResponseEntity<WorkspaceUserRolesResponse> shareWorkspace(
      String workspaceNamespace, String workspaceId, ShareWorkspaceRequest request) {
    if (Strings.isNullOrEmpty(request.getWorkspaceEtag())) {
      throw new BadRequestException("Missing required update field 'workspaceEtag'");
    }

    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    List<UserRole> userRolesBeforeShare =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    int version = Etags.toVersion(request.getWorkspaceEtag());
    if (dbWorkspace.getVersion() != version) {
      throw new ConflictException("Attempted to modify user roles with outdated workspace etag");
    }

    ImmutableMap.Builder<String, WorkspaceAccessLevel> shareRolesMapBuilder =
        new ImmutableMap.Builder<>();
    ImmutableMap.Builder<Long, String> aclStringsByUserIdBuilder = new ImmutableMap.Builder<>();

    for (UserRole role : request.getItems()) {
      if (role.getRole() == null || role.getRole().toString().trim().isEmpty()) {
        throw new BadRequestException("Role required.");
      }
      final DbUser invitedUser = userDao.findUserByUsername(role.getEmail());
      if (invitedUser == null) {
        throw new BadRequestException(String.format("User %s doesn't exist", role.getEmail()));
      }

      aclStringsByUserIdBuilder.put(invitedUser.getUserId(), role.getRole().toString());
      shareRolesMapBuilder.put(role.getEmail(), role.getRole());
    }
    final ImmutableMap<String, WorkspaceAccessLevel> aclsByEmail = shareRolesMapBuilder.build();

    // This automatically enforces the "canShare" permission.
    dbWorkspace = workspaceAuthService.updateWorkspaceAcls(dbWorkspace, aclsByEmail);
    WorkspaceUserRolesResponse resp = new WorkspaceUserRolesResponse();
    resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.getVersion()));

    List<UserRole> userRolesAfterShare =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    resp.setItems(userRolesAfterShare);

    if (dbWorkspace.getCdrVersion().getAccessTier().getEnableUserWorkflows()) {
      List<String> usersHavePermission =
          aclsByEmail.entrySet().stream()
              .filter(entry -> shouldGrantWorkflowRunnerAsService(userProvider.get(), entry))
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      // Find the users who are owners or writers before share, but not in the new gain permission
      // list
      List<String> userLostPermission  = userRolesBeforeShare.stream().filter(u -> (u.getRole().equals(WorkspaceAccessLevel.OWNER) || u.getRole().equals(WorkspaceAccessLevel.OWNER)) && !usersHavePermission.contains(u.getEmail()))
              .map(u -> u.getEmail()).collect(Collectors.toList());
      iamService.grantWorkflowRunnerRoleToUsers(
          dbWorkspace.getGoogleProject(), usersHavePermission);
      iamService.revokeWorkflowRunnerRoleToUsers(
          dbWorkspace.getGoogleProject(), userLostPermission);
    }

    workspaceAuditor.fireCollaborateAction(
        dbWorkspace.getWorkspaceId(), aclStringsByUserIdBuilder.build());
    return ResponseEntity.ok(resp);
  }

  @Override
  public ResponseEntity<Workspace> markResearchPurposeReviewed(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    dbWorkspace.setNeedsReviewPrompt(false);
    try {
      dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace);
    } catch (Exception e) {
      throw e;
    }
    return ResponseEntity.ok(
        workspaceMapper.toApiWorkspace(
            dbWorkspace,
            fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getWorkspace()));
  }

  @Override
  public ResponseEntity<WorkspaceUserRolesResponse> getFirecloudWorkspaceUserRoles(
      String workspaceNamespace, String workspaceId) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);

    List<UserRole> userRoles =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    return ResponseEntity.ok(new WorkspaceUserRolesResponse().items(userRoles));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getPublishedWorkspaces() {
    WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(workspaceService.getPublishedWorkspaces());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RecentWorkspaceResponse> getUserRecentWorkspaces() {
    List<DbUserRecentWorkspace> userRecentWorkspaces = workspaceService.getRecentWorkspaces();
    List<Long> workspaceIds =
        userRecentWorkspaces.stream()
            .map(DbUserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<DbWorkspace> dbWorkspaces = workspaceDao.findAllByWorkspaceIdIn(workspaceIds);
    Map<Long, DbWorkspace> dbWorkspacesById =
        dbWorkspaces.stream()
            .collect(Collectors.toMap(DbWorkspace::getWorkspaceId, Function.identity()));
    final Map<Long, WorkspaceAccessLevel> workspaceAccessLevelsById;
    try {
      workspaceAccessLevelsById =
          dbWorkspaces.stream()
              .collect(
                  Collectors.toMap(
                      DbWorkspace::getWorkspaceId,
                      dbWorkspace ->
                          workspaceAuthService.getWorkspaceAccessLevel(
                              dbWorkspace.getWorkspaceNamespace(),
                              dbWorkspace.getFirecloudName())));

    } catch (IllegalArgumentException e) {
      throw new ServerErrorException(e);
    }

    RecentWorkspaceResponse recentWorkspaceResponse = new RecentWorkspaceResponse();
    List<RecentWorkspace> recentWorkspaces =
        userRecentWorkspaces.stream()
            .map(
                userRecentWorkspace ->
                    workspaceMapper.toApiRecentWorkspace(
                        dbWorkspacesById.get(userRecentWorkspace.getWorkspaceId()),
                        workspaceAccessLevelsById.get(userRecentWorkspace.getWorkspaceId())))
            .collect(Collectors.toList());
    recentWorkspaceResponse.addAll(recentWorkspaces);
    return ResponseEntity.ok(recentWorkspaceResponse);
  }

  @Override
  public ResponseEntity<RecentWorkspaceResponse> updateRecentWorkspaces(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    workspaceService.updateRecentWorkspaces(dbWorkspace);
    final WorkspaceAccessLevel workspaceAccessLevel;

    try {
      workspaceAccessLevel =
          workspaceAuthService.getWorkspaceAccessLevel(workspaceNamespace, workspaceId);
    } catch (IllegalArgumentException e) {
      throw new ServerErrorException(e);
    }

    RecentWorkspaceResponse recentWorkspaceResponse = new RecentWorkspaceResponse();
    RecentWorkspace recentWorkspace =
        workspaceMapper.toApiRecentWorkspace(dbWorkspace, workspaceAccessLevel);
    recentWorkspaceResponse.add(recentWorkspace);
    return ResponseEntity.ok(recentWorkspaceResponse);
  }

  @Override
  public ResponseEntity<WorkspaceResourceResponse> getWorkspaceResources(
      String workspaceNamespace,
      String workspaceId,
      WorkspaceResourcesRequest workspaceResourcesRequest) {
    WorkspaceAccessLevel workspaceAccessLevel =
        workspaceAuthService.enforceWorkspaceAccessLevel(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    final DbWorkspace dbWorkspace =
        workspaceDao.getRequiredWithCohorts(workspaceNamespace, workspaceId);
    // When loading resources we are not accessing CDR tables for concept sets
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(dbWorkspace.getCdrVersion());
    WorkspaceResourceResponse workspaceResourceResponse = new WorkspaceResourceResponse();
    workspaceResourceResponse.addAll(
        workspaceResourcesService.getWorkspaceResources(
            dbWorkspace, workspaceAccessLevel, workspaceResourcesRequest.getTypesToFetch()));
    return ResponseEntity.ok(workspaceResourceResponse);
  }

  public ResponseEntity<WorkspaceCreatorFreeCreditsRemainingResponse>
      getWorkspaceCreatorFreeCreditsRemaining(String workspaceNamespace, String workspaceId) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    double freeCreditsRemaining =
        freeTierBillingService.getWorkspaceCreatorFreeCreditsRemaining(dbWorkspace);
    return ResponseEntity.ok(
        new WorkspaceCreatorFreeCreditsRemainingResponse()
            .freeCreditsRemaining(freeCreditsRemaining));
  }

  /** Returns {@true} if the user is 1: workspace OWNER or WRITER; 2: NOT current logged in user */
  private static boolean shouldGrantWorkflowRunnerAsService(
      DbUser loggedInUser, Map.Entry<String, WorkspaceAccessLevel> userNameToAclMapEntry) {
    return !userNameToAclMapEntry.getKey().equals(loggedInUser.getUsername())
        && (userNameToAclMapEntry.getValue().equals(WorkspaceAccessLevel.OWNER)
            || userNameToAclMapEntry.getValue().equals(WorkspaceAccessLevel.WRITER));
  }
}
