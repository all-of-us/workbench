package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.EmptyBufferException;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.DbWorkspace.FirecloudWorkspaceId;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.TooManyRequestsException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.RecentWorkspaceResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResearchPurposeReviewRequest;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceBillingUsageResponse;
import org.pmiops.workbench.model.WorkspaceCreatorFreeCreditsRemainingResponse;
import org.pmiops.workbench.model.WorkspaceListResponse;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.model.WorkspaceResourcesRequest;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.model.WorkspaceUserRolesResponse;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.notebooks.BlobAlreadyExistsException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.RandomUtils;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspacesController implements WorkspacesApiDelegate {

  private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

  private static final int NUM_RANDOM_CHARS = 20;
  private static final Level OPERATION_TIME_LOG_LEVEL = Level.FINE;
  private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz";

  private final BillingProjectBufferService billingProjectBufferService;
  private final CdrVersionDao cdrVersionDao;
  private final Clock clock;
  private final CloudStorageClient cloudStorageClient;
  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final LogsBasedMetricService logsBasedMetricService;
  private final NotebooksService notebooksService;
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

  @Autowired
  public WorkspacesController(
      BillingProjectBufferService billingProjectBufferService,
      CdrVersionDao cdrVersionDao,
      Clock clock,
      CloudStorageClient cloudStorageClient,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      LogsBasedMetricService logsBasedMetricService,
      NotebooksService notebooksService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      WorkspaceAuditor workspaceAuditor,
      WorkspaceMapper workspaceMapper,
      WorkspaceResourcesService workspaceResourcesService,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService,
      WorkspaceAuthService workspaceAuthService) {
    this.billingProjectBufferService = billingProjectBufferService;
    this.cdrVersionDao = cdrVersionDao;
    this.clock = clock;
    this.cloudStorageClient = cloudStorageClient;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.logsBasedMetricService = logsBasedMetricService;
    this.notebooksService = notebooksService;
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
  }

  private DbCdrVersion getLiveCdrVersionId(String cdrVersionId) {
    if (Strings.isNullOrEmpty(cdrVersionId)) {
      throw new BadRequestException("missing cdrVersionId");
    }
    try {
      DbCdrVersion cdrVersion = cdrVersionDao.findOne(Long.parseLong(cdrVersionId));
      if (cdrVersion == null) {
        throw new BadRequestException(
            String.format("CDR version with ID %s not found", cdrVersionId));
      }
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
    // Find a unique workspace namespace based off of the provided name.
    String strippedName = name.toLowerCase().replaceAll("[^0-9a-z]", "");
    // If the stripped name has no chars, generate a random name.
    if (strippedName.isEmpty()) {
      strippedName = RandomUtils.generateRandomChars(RANDOM_CHARS, NUM_RANDOM_CHARS);
    }
    return new FirecloudWorkspaceId(namespace, strippedName);
  }

  @Override
  public ResponseEntity<Workspace> createWorkspace(Workspace workspace) throws BadRequestException {
    return ResponseEntity.ok(
        recordOperationTime(() -> createWorkspaceImpl(workspace), "createWorkspace"));
  }

  // TODO(jaycarlton): migrate this and other "impl" methods to WorkspaceService &
  // WorkspaceServiceImpl
  private Workspace createWorkspaceImpl(Workspace workspace) {
    validateWorkspaceApiModel(workspace);

    DbCdrVersion cdrVersion = getLiveCdrVersionId(workspace.getCdrVersionId());
    DbAccessTier accessTier = cdrVersion.getAccessTier();
    DbUser user = userProvider.get();

    // Note: please keep any initialization logic here in sync with cloneWorkspaceImpl().
    final String billingProject = claimBillingProject(user, accessTier);
    FirecloudWorkspaceId workspaceId =
        generateFirecloudWorkspaceId(billingProject, workspace.getName());
    FirecloudWorkspace fcWorkspace =
        fireCloudService.createWorkspace(
            workspaceId.getWorkspaceNamespace(),
            workspaceId.getWorkspaceName(),
            accessTier.getAuthDomainName());

    DbWorkspace dbWorkspace =
        createDbWorkspace(workspace, cdrVersion, user, workspaceId, fcWorkspace);
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

    final Workspace createdWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);
    workspaceAuditor.fireCreateAction(createdWorkspace, dbWorkspace.getWorkspaceId());
    return createdWorkspace;
  }

  private DbWorkspace createDbWorkspace(
      Workspace workspace,
      DbCdrVersion cdrVersion,
      DbUser user,
      FirecloudWorkspaceId workspaceId,
      FirecloudWorkspace fcWorkspace) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    // The final step in the process is to clone the AoU representation of the
    // workspace. The implication here is that we may generate orphaned
    // Firecloud workspaces / buckets, but a user should not be able to see
    // half-way cloned workspaces via AoU - so it will just appear as a
    // transient failure.
    DbWorkspace dbWorkspace = new DbWorkspace();

    dbWorkspace.setName(workspace.getName());
    dbWorkspace.setCreator(user);
    dbWorkspace.setFirecloudName(workspaceId.getWorkspaceName());
    dbWorkspace.setWorkspaceNamespace(workspaceId.getWorkspaceNamespace());
    dbWorkspace.setFirecloudUuid(fcWorkspace.getWorkspaceId());
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.NEW);
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
      // Will be addressed with RW-4440
      throw new ServerErrorException(
          "This message is going to be swallowed due to a bug in ExceptionAdvice. ",
          new ServerErrorException("Could not update the workspace's billing account", e));
    }

    return dbWorkspace;
  }

  private String claimBillingProject(DbUser user, DbAccessTier accessTier) {
    try {
      final DbBillingProjectBufferEntry bufferedBillingProject =
          billingProjectBufferService.assignBillingProject(user, accessTier);
      return bufferedBillingProject.getFireCloudProjectName();
    } catch (EmptyBufferException e) {
      throw new TooManyRequestsException(e);
    }
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
    recordOperationTime(
        () -> {
          DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
          workspaceService.deleteWorkspace(dbWorkspace);
          workspaceAuditor.fireDeleteAction(dbWorkspace);
        },
        "deleteWorkspace");
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(
        recordOperationTime(
            () -> workspaceService.getWorkspace(workspaceNamespace, workspaceId), "getWorkspace"));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    final WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(recordOperationTime(workspaceService::getWorkspaces, "getWorkspaces"));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Workspace> updateWorkspace(
      String workspaceNamespace, String workspaceId, UpdateWorkspaceRequest request)
      throws NotFoundException {
    final Workspace result =
        recordOperationTime(
            () -> updateWorkspaceImpl(workspaceNamespace, workspaceId, request), "updateWorkspace");
    return ResponseEntity.ok(result);
  }

  private Workspace updateWorkspaceImpl(
      String workspaceNamespace, String workspaceId, UpdateWorkspaceRequest request) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER);
    Workspace workspace = request.getWorkspace();
    FirecloudWorkspace fcWorkspace =
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
        // Will be addressed with RW-4440
        throw new ServerErrorException(
            "This message is going to be swallowed due to a bug in ExceptionAdvice.",
            new ServerErrorException("Could not update the workspace's billing account", e));
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
    return workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);
  }

  @Override
  public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(
      String fromWorkspaceNamespace, String fromWorkspaceId, CloneWorkspaceRequest body)
      throws BadRequestException, TooManyRequestsException {
    return recordOperationTime(
        () -> cloneWorkspaceImpl(fromWorkspaceNamespace, fromWorkspaceId, body), "cloneWorkspace");
  }

  private ResponseEntity<CloneWorkspaceResponse> cloneWorkspaceImpl(
      String fromWorkspaceNamespace, String fromWorkspaceId, CloneWorkspaceRequest body) {
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
    final String billingProject = claimBillingProject(user, accessTier);
    FirecloudWorkspaceId toFcWorkspaceId =
        generateFirecloudWorkspaceId(billingProject, toWorkspace.getName());
    FirecloudWorkspace toFcWorkspace =
        fireCloudService.cloneWorkspace(
            fromWorkspaceNamespace,
            fromWorkspaceId,
            toFcWorkspaceId.getWorkspaceNamespace(),
            toFcWorkspaceId.getWorkspaceName(),
            accessTier.getAuthDomainName());

    DbWorkspace dbWorkspace =
        createDbWorkspace(toWorkspace, toCdrVersion, user, toFcWorkspaceId, toFcWorkspace);

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
    if (Optional.ofNullable(body.getIncludeUserRoles()).orElse(false)) {
      Map<String, FirecloudWorkspaceAccessEntry> fromAclsMap =
          workspaceAuthService.getFirecloudWorkspaceAcls(
              fromWorkspace.getWorkspaceNamespace(), fromWorkspace.getFirecloudName());

      Map<String, WorkspaceAccessLevel> clonedRoles = new HashMap<>();
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

    final Workspace savedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, toFcWorkspace);
    workspaceAuditor.fireDuplicateAction(
        fromWorkspace.getWorkspaceId(), dbWorkspace.getWorkspaceId(), savedWorkspace);
    return ResponseEntity.ok(new CloneWorkspaceResponse().workspace(savedWorkspace));
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

    List<UserRole> updatedUserRoles =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    resp.setItems(updatedUserRoles);

    workspaceAuditor.fireCollaborateAction(
        dbWorkspace.getWorkspaceId(), aclStringsByUserIdBuilder.build());
    return ResponseEntity.ok(resp);
  }

  /** Record approval or rejection of research purpose. */
  @Override
  @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
  public ResponseEntity<EmptyResponse> reviewWorkspace(
      String ns, String id, ResearchPurposeReviewRequest review) {
    DbWorkspace workspace = workspaceDao.getRequired(ns, id);
    userService.logAdminWorkspaceAction(
        workspace.getWorkspaceId(),
        "research purpose approval",
        workspace.getApproved(),
        review.getApproved());
    workspaceService.setResearchPurposeApproved(ns, id, review.getApproved());
    return ResponseEntity.ok(new EmptyResponse());
  }

  // Note we do not paginate the workspaces list, since we expect few workspaces
  // to require review.
  //
  // We can add pagination in the DAO by returning Slice<DbWorkspace> if we want the method to
  // return
  // pagination information (e.g. are there more workspaces to get), and Page<DbWorkspace> if we
  // want the method to return both pagination information and a total count.
  @Override
  @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
  public ResponseEntity<WorkspaceListResponse> getWorkspacesForReview() {
    WorkspaceListResponse response = new WorkspaceListResponse();
    List<DbWorkspace> workspaces = findForReview();
    response.setItems(
        workspaces.stream().map(workspaceMapper::toApiWorkspace).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  private List<DbWorkspace> findForReview() {
    return workspaceDao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  }

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(
        recordOperationTime(
            () -> notebooksService.getNotebooks(workspaceNamespace, workspaceId),
            "getNoteBookList"));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteNotebook(
      String workspace, String workspaceName, String notebookName) {
    recordOperationTime(
        () -> notebooksService.deleteNotebook(workspace, workspaceName, notebookName),
        "deleteNotebook");
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromNotebookName,
      CopyRequest copyRequest) {
    return ResponseEntity.ok(
        recordOperationTime(
            () ->
                copyNotebookImpl(
                    fromWorkspaceNamespace, fromWorkspaceId, fromNotebookName, copyRequest),
            "copyNotebook"));
  }

  private FileDetail copyNotebookImpl(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromNotebookName,
      CopyRequest copyRequest) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.copyNotebook(
              fromWorkspaceNamespace,
              fromWorkspaceId,
              NotebooksService.withNotebookExtension(fromNotebookName),
              copyRequest.getToWorkspaceNamespace(),
              copyRequest.getToWorkspaceName(),
              NotebooksService.withNotebookExtension(copyRequest.getNewName()));
    } catch (BlobAlreadyExistsException e) {
      throw new ConflictException("File already exists at copy destination");
    }
    return fileDetail;
  }

  @Override
  public ResponseEntity<FileDetail> cloneNotebook(
      String workspace, String workspaceName, String notebookName) {
    FileDetail fileDetail;
    try {
      fileDetail = notebooksService.cloneNotebook(workspace, workspaceName, notebookName);
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<FileDetail> renameNotebook(
      String workspace, String workspaceName, NotebookRename rename) {
    FileDetail fileDetail;
    try {
      fileDetail =
          notebooksService.renameNotebook(
              workspace, workspaceName, rename.getName(), rename.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<KernelTypeEnum> getNotebookKernel(
      String workspace, String workspaceName, String notebookName) {
    return ResponseEntity.ok(
        notebooksService.getNotebookKernel(workspace, workspaceName, notebookName));
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
  public ResponseEntity<ReadOnlyNotebookResponse> readOnlyNotebook(
      String workspaceNamespace, String workspaceName, String notebookName) {
    ReadOnlyNotebookResponse response =
        new ReadOnlyNotebookResponse()
            .html(
                notebooksService.getReadOnlyHtml(workspaceNamespace, workspaceName, notebookName));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<NotebookLockingMetadataResponse> getNotebookLockingMetadata(
      String workspaceNamespace, String workspaceName, String notebookName) {

    // Retrieving the workspace is done first, which acts as an access check.
    String bucketName =
        fireCloudService
            .getWorkspace(workspaceNamespace, workspaceName)
            .getWorkspace()
            .getBucketName();

    // response may be empty - fill in what we can
    NotebookLockingMetadataResponse response = new NotebookLockingMetadataResponse();

    // throws NotFoundException if the notebook is not in GCS
    // returns null if found but no user-metadata
    Map<String, String> metadata =
        cloudStorageClient.getMetadata(bucketName, "notebooks/" + notebookName);

    if (metadata != null) {
      String lockExpirationTime = metadata.get("lockExpiresAt");
      if (lockExpirationTime != null) {
        response.lockExpirationTime(Long.valueOf(lockExpirationTime));
      }

      // stored as a SHA-256 hash of bucketName:userEmail
      String lastLockedByHash = metadata.get("lastLockedBy");
      if (lastLockedByHash != null) {

        // the caller should not necessarily know the identities of all notebook users
        // so we check against the set of users of this workspace which are known to the caller

        // NOTE: currently, users of workspace X of any access level can see all other
        // workspace X users. This is not desired.
        // https://precisionmedicineinitiative.atlassian.net/browse/RW-3094

        Set<String> workspaceUsers =
            workspaceAuthService
                .getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName)
                .keySet();

        response.lastLockedBy(findHashedUser(bucketName, workspaceUsers, lastLockedByHash));
      }
    }

    // If a lock is held by another user, log this to establish a rough estimate of how often
    // locked notebooks are encountered. Note that this only covers locks encountered from the
    // Workbench - any Jupyter UI-based lock detection does not touch this code path.
    String currentUsername = userProvider.get().getUsername();
    if (response.getLockExpirationTime() == null
        || response.getLastLockedBy() == null
        || response.getLockExpirationTime() < clock.millis()
        || response.getLastLockedBy().equals(currentUsername)) {
      log.info(String.format("user '%s' observed notebook available for editing", currentUsername));
    } else {
      log.info(
          String.format(
              "user '%s' observed notebook locked by '%s'",
              currentUsername, response.getLastLockedBy()));
    }

    return ResponseEntity.ok(response);
  }

  private String findHashedUser(String bucket, Set<String> workspaceUsers, String hash) {
    return workspaceUsers.stream()
        .filter(email -> notebookLockingEmailHash(bucket, email).equals(hash))
        .findAny()
        .orElse("UNKNOWN");
  }

  @VisibleForTesting
  static String notebookLockingEmailHash(String bucket, String email) {
    String toHash = String.format("%s:%s", bucket, email);
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] hash = sha256.digest(toHash.getBytes(StandardCharsets.UTF_8));
      // convert to printable hex text
      return BaseEncoding.base16().lowerCase().encode(hash);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ResponseEntity<WorkspaceUserRolesResponse> getFirecloudWorkspaceUserRoles(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceId);

    List<UserRole> userRoles =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    WorkspaceUserRolesResponse resp = new WorkspaceUserRolesResponse();
    resp.setItems(userRoles);
    return ResponseEntity.ok(resp);
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getPublishedWorkspaces() {
    WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(workspaceService.getPublishedWorkspaces());
    return ResponseEntity.ok(response);
  }

  @Override
  @AuthorityRequired({Authority.FEATURED_WORKSPACE_ADMIN})
  public ResponseEntity<EmptyResponse> publishWorkspace(
      String workspaceNamespace, String workspaceId) {
    workspaceService.setPublished(workspaceNamespace, workspaceId, true);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.FEATURED_WORKSPACE_ADMIN})
  public ResponseEntity<EmptyResponse> unpublishWorkspace(
      String workspaceNamespace, String workspaceId) {
    workspaceService.setPublished(workspaceNamespace, workspaceId, false);
    return ResponseEntity.ok(new EmptyResponse());
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

  private <T> T recordOperationTime(Supplier<T> operation, String operationName) {
    log.log(OPERATION_TIME_LOG_LEVEL, String.format("recordOperationTime: %s", operationName));
    return logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder().addTag(MetricLabel.OPERATION_NAME, operationName),
        DistributionMetric.WORKSPACE_OPERATION_TIME,
        operation);
  }

  private void recordOperationTime(Runnable operation, String operationName) {
    log.log(OPERATION_TIME_LOG_LEVEL, String.format("recordOperationTime: %s", operationName));
    logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder().addTag(MetricLabel.OPERATION_NAME, operationName),
        DistributionMetric.WORKSPACE_OPERATION_TIME,
        operation);
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
}
