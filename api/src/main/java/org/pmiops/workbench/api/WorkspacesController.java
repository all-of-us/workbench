package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.db.model.DbWorkspaceOperation.DbWorkspaceOperationStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.TooManyRequestsException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.CreateWorkspaceTaskRequest;
import org.pmiops.workbench.model.DuplicateWorkspaceTaskRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.RecentWorkspaceResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceBillingUsageResponse;
import org.pmiops.workbench.model.WorkspaceCreatorFreeCreditsRemainingResponse;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.model.WorkspaceUserRolesResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourcesService;
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
  private final IamService iamService;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final TaskQueueService taskQueueService;
  private final UserDao userDao;
  private final WorkspaceAuditor workspaceAuditor;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceOperationDao workspaceOperationDao;
  private final WorkspaceOperationMapper workspaceOperationMapper;
  private final WorkspaceResourcesService workspaceResourcesService;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspacesController(
      CdrVersionDao cdrVersionDao,
      Clock clock,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      IamService iamService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService,
      UserDao userDao,
      WorkspaceAuditor workspaceAuditor,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceOperationDao workspaceOperationDao,
      WorkspaceOperationMapper workspaceOperationMapper,
      WorkspaceResourcesService workspaceResourcesService,
      WorkspaceService workspaceService) {
    this.cdrVersionDao = cdrVersionDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.iamService = iamService;
    this.taskQueueService = taskQueueService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuditor = workspaceAuditor;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceOperationDao = workspaceOperationDao;
    this.workspaceOperationMapper = workspaceOperationMapper;
    this.workspaceResourcesService = workspaceResourcesService;
    this.workspaceService = workspaceService;
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

  @Override
  public ResponseEntity<Workspace> createWorkspace(Workspace workspace) throws BadRequestException {
    validateWorkspaceApiModel(workspace);

    DbCdrVersion cdrVersion = getLiveCdrVersionId(workspace.getCdrVersionId());
    DbAccessTier accessTier = cdrVersion.getAccessTier();
    DbUser user = userProvider.get();

    // Note: please keep any initialization logic here in sync with cloneWorkspaceImpl().
    String billingProject = createTerraBillingProject(accessTier);
    String firecloudName = FireCloudService.toFirecloudName(workspace.getName());

    RawlsWorkspaceDetails fcWorkspace =
        fireCloudService.createWorkspace(
            billingProject, firecloudName, accessTier.getAuthDomainName());
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
    final Workspace createdWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace);
    workspaceAuditor.fireCreateAction(createdWorkspace, dbWorkspace.getWorkspaceId());

    if (cdrVersion.getTanagraEnabled()) {
      try {
        workspaceService.createTanagraStudy(
            createdWorkspace.getNamespace(), createdWorkspace.getName());
      } catch (Exception e) {
        log.log(
            Level.SEVERE,
            String.format(
                "Could not create a Tanagra study for workspace namespace: %s, name: %s",
                createdWorkspace.getNamespace(), createdWorkspace.getName()),
            e);
      }
    }
    return ResponseEntity.ok(createdWorkspace);
  }

  private DbWorkspaceOperation initWorkspaceOperation() {
    DbWorkspaceOperation operation =
        workspaceOperationDao.save(
            new DbWorkspaceOperation()
                .setCreatorId(userProvider.get().getUserId())
                .setStatus(DbWorkspaceOperationStatus.QUEUED));
    return operation;
  }

  @Override
  public ResponseEntity<WorkspaceOperation> createWorkspaceAsync(Workspace workspace) {
    // Basic request validation.
    validateWorkspaceApiModel(workspace);
    getLiveCdrVersionId(workspace.getCdrVersionId());

    // TODO: enforce access level check here? Not strictly necessary, but may make sense as
    // belt/suspenders check.

    DbWorkspaceOperation operation = initWorkspaceOperation();

    log.info(
        String.format(
            "Create Workspace Async: created operation %d in %s state",
            operation.getId(), operation.getStatus().toString()));

    taskQueueService.pushCreateWorkspaceTask(operation.getId(), workspace);
    return ResponseEntity.ok(workspaceOperationMapper.toModelWithoutWorkspace(operation));
  }

  @Override
  public ResponseEntity<WorkspaceOperation> duplicateWorkspaceAsync(
      String fromWorkspaceNamespace, String fromWorkspaceId, CloneWorkspaceRequest request) {

    // Basic request validation.
    validateWorkspaceApiModel(request.getWorkspace());
    getLiveCdrVersionId(request.getWorkspace().getCdrVersionId());

    // Verify the caller has read access to the source workspace.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);

    DbWorkspaceOperation operation = initWorkspaceOperation();

    log.info(
        String.format(
            "Duplicate Workspace Async: created operation %d in %s state",
            operation.getId(), operation.getStatus().toString()));

    taskQueueService.pushDuplicateWorkspaceTask(
        operation.getId(),
        fromWorkspaceNamespace,
        fromWorkspaceId,
        request.getIncludeUserRoles(),
        request.getWorkspace());
    return ResponseEntity.ok(workspaceOperationMapper.toModelWithoutWorkspace(operation));
  }

  @Override
  public ResponseEntity<WorkspaceOperation> getWorkspaceOperation(Long id) {
    return workspaceOperationDao
        .findById(id)
        // only callable by the creator
        .filter(dbOperation -> dbOperation.getCreatorId() == userProvider.get().getUserId())
        .map(
            op ->
                ResponseEntity.ok()
                    .body(
                        workspaceOperationMapper.toModelWithWorkspace(
                            op, workspaceDao, fireCloudService, workspaceMapper)))
        .orElse(ResponseEntity.notFound().build());
  }

  private void processWorkspaceTask(long operationId, Supplier<Workspace> workspaceAction) {
    DbWorkspaceOperation operation =
        workspaceOperationDao
            .findById(operationId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Workspace Operation '%d' not found", operationId)));

    if (operation.getStatus() != DbWorkspaceOperationStatus.QUEUED) {
      log.warning(
          String.format(
              "processWorkspaceTask: exiting because operation %d is in %s state instead of QUEUED",
              operation.getId(), operation.getStatus().toString()));
      return;
    }

    try {
      log.info(
          String.format(
              "processWorkspaceTask: begin processing operation %d by transitioning from %s to %s",
              operation.getId(),
              operation.getStatus().toString(),
              DbWorkspaceOperationStatus.PROCESSING.toString()));
      operation =
          workspaceOperationDao.save(operation.setStatus(DbWorkspaceOperationStatus.PROCESSING));

      Workspace w = workspaceAction.get();
      // careful: w.getId() refers to the Terra Name, not the DB ID
      long workspaceId = workspaceDao.getRequired(w.getNamespace(), w.getId()).getWorkspaceId();
      log.info(
          String.format(
              "processWorkspaceTask: recording SUCCESS for operation %d - workspace ID %d",
              operation.getId(), workspaceId));
      operation.setStatus(DbWorkspaceOperationStatus.SUCCESS).setWorkspaceId(workspaceId);
    } catch (Exception e) {
      log.info(
          String.format(
              "processWorkspaceTask: recording ERROR for operation %d", operation.getId()));
      operation.setStatus(DbWorkspaceOperationStatus.ERROR);
      throw e;
    } finally {
      operation = workspaceOperationDao.save(operation);
    }
  }

  @Override
  public ResponseEntity<Void> processCreateWorkspaceTask(CreateWorkspaceTaskRequest request) {
    processWorkspaceTask(
        request.getOperationId(),
        () -> {
          log.info(
              String.format(
                  "processCreateWorkspaceTask: creating workspace for operation %d",
                  request.getOperationId()));
          return this.createWorkspace(request.getWorkspace()).getBody();
        });
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> processDuplicateWorkspaceTask(DuplicateWorkspaceTaskRequest request) {
    processWorkspaceTask(
        request.getOperationId(),
        () -> {
          log.info(
              String.format(
                  "processDuplicateWorkspaceTask: duplicating workspace for operation %d",
                  request.getOperationId()));
          return this.cloneWorkspace(
                  request.getFromWorkspaceNamespace(),
                  request.getFromWorkspaceFirecloudName(),
                  new CloneWorkspaceRequest()
                      .workspace(request.getWorkspace())
                      .includeUserRoles(request.getShouldDuplicateRoles()))
              .getBody()
              .getWorkspace();
        });
    return ResponseEntity.ok().build();
  }

  private DbWorkspace createDbWorkspace(
      Workspace workspace,
      DbCdrVersion cdrVersion,
      DbUser user,
      RawlsWorkspaceDetails fcWorkspace) {
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
    dbWorkspace.setLastModifiedBy(userProvider.get().getUsername());
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
      throw new ServerErrorException("Could not update the workspace's billing account", e);
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
    RawlsWorkspaceDetails fcWorkspace =
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

    if (workspace.getAccessTierShortName() != null
        && !dbWorkspace
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
        throw new ServerErrorException("Could not update the workspace's billing account", e);
      }
    }

    try {
      // The version asserted on save is the same as the one we read via
      // getRequired() above, see RW-215 for details.
      dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace, userProvider.get());
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
    String billingProject = createTerraBillingProject(accessTier);
    String firecloudName = FireCloudService.toFirecloudName(toWorkspace.getName());
    RawlsWorkspaceDetails toFcWorkspace =
        fireCloudService.cloneWorkspace(
            fromWorkspaceNamespace,
            fromWorkspaceId,
            billingProject,
            firecloudName,
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
    if (Boolean.TRUE.equals(body.getIncludeUserRoles())) {
      var fromAcl =
          workspaceAuthService.getFirecloudWorkspaceAcl(
              fromWorkspace.getWorkspaceNamespace(), fromWorkspace.getFirecloudName());

      final String cloningUser = user.getUsername();
      final String publishedGroup = workspaceService.getPublishedWorkspacesGroupEmail();
      var toAcl =
          Maps.transformEntries(
              fromAcl,
              (username, accessEntry) -> {
                // RW-9501: cloned workspaces should not be published
                if (username.equals(publishedGroup)) return WorkspaceAccessLevel.NO_ACCESS;
                // the cloning user is the creator of the new workspace (hence also an OWNER)
                if (username.equals(cloningUser)) return WorkspaceAccessLevel.OWNER;
                // all other users retain the same access
                return WorkspaceAccessLevel.fromValue(accessEntry.getAccessLevel());
              });

      dbWorkspace = workspaceAuthService.patchWorkspaceAcl(dbWorkspace, toAcl);
    }

    // RW-9501: cloned workspaces should not be published
    dbWorkspace = dbWorkspace.setPublished(false);

    dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace, user);
    final Workspace savedWorkspace = workspaceMapper.toApiWorkspace(dbWorkspace, toFcWorkspace);

    workspaceAuditor.fireDuplicateAction(
        fromWorkspace.getWorkspaceId(), dbWorkspace.getWorkspaceId(), savedWorkspace);

    return ResponseEntity.ok(new CloneWorkspaceResponse().workspace(savedWorkspace));
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
  public ResponseEntity<WorkspaceUserRolesResponse> shareWorkspacePatch(
      String workspaceNamespace, String workspaceId, ShareWorkspaceRequest request) {
    if (Strings.isNullOrEmpty(request.getWorkspaceEtag())) {
      throw new BadRequestException("Missing required update field 'workspaceEtag'");
    }

    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER);

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

    WorkspaceUserRolesResponse resp = new WorkspaceUserRolesResponse();

    DbUser currentUser = userProvider.get();
    // Revoke lifescience permission before asking Firecloud to remove users; after unsharing
    // in Firecloud, we can no longer get the user's petSA from SAM using their credentials.
    if (dbWorkspace.getCdrVersion().getAccessTier().getEnableUserWorkflows()) {
      List<String> finalWorkflowUsers =
          aclsByEmail.entrySet().stream()
              .filter(entry -> shouldGrantWorkflowRunnerAsService(currentUser, entry))
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      // Find the users who are owners or writers before share, but not in the new gain permission
      // list
      List<String> userLostPermission =
          userRolesBeforeShare.stream()
              .filter(
                  u ->
                      (WorkspaceAccessLevel.OWNER.equals(u.getRole())
                              || WorkspaceAccessLevel.WRITER.equals(u.getRole()))
                          && !finalWorkflowUsers.contains(u.getEmail())
                          && !u.getEmail().equals(currentUser.getUsername()))
              .map(UserRole::getEmail)
              .collect(Collectors.toList());
      List<String> failedRevocations =
          iamService.revokeWorkflowRunnerRoleForUsers(
              dbWorkspace.getGoogleProject(), userLostPermission);
      if (!failedRevocations.isEmpty()) {
        resp.setFailedWorkflowRevocations(failedRevocations);
      }
    }

    dbWorkspace = workspaceAuthService.patchWorkspaceAcl(dbWorkspace, aclsByEmail);
    resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.getVersion()));

    List<UserRole> userRolesAfterShare =
        workspaceService.getFirecloudUserRoles(workspaceNamespace, dbWorkspace.getFirecloudName());
    resp.setItems(userRolesAfterShare);

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
      dbWorkspace = workspaceDao.saveWithLastModified(dbWorkspace, userProvider.get());
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
  public ResponseEntity<WorkspaceResourceResponse> getWorkspaceResourcesV2(
      String ns, String id, List<String> rtStrings) {
    return getWorkspaceResourcesImpl(
        ns, id, rtStrings.stream().map(ResourceType::fromValue).collect(Collectors.toList()));
  }

  ResponseEntity<WorkspaceResourceResponse> getWorkspaceResourcesImpl(
      String workspaceNamespace, String workspaceId, List<ResourceType> resourceTypesToFetch) {
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
            dbWorkspace, workspaceAccessLevel, resourceTypesToFetch));
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
        && (WorkspaceAccessLevel.OWNER.equals(userNameToAclMapEntry.getValue())
            || WorkspaceAccessLevel.WRITER.equals(userNameToAclMapEntry.getValue()));
  }
}
