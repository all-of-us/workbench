package org.pmiops.workbench.workspaces;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.StorageException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.api.WorkspacesApiDelegate;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.EmptyBufferException;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.TooManyRequestsException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.notebooks.BlobAlreadyExistsException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspacesController implements WorkspacesApiDelegate {

  private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

  private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final int NUM_RANDOM_CHARS = 20;
  // If we later decide to tune this value, consider moving to the WorkbenchConfig.
  private static final int MAX_CLONE_FILE_SIZE_MB = 100;

  private Retryer<Boolean> retryer =
      RetryerBuilder.<Boolean>newBuilder()
          .retryIfExceptionOfType(StorageException.class)
          .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
          .withStopStrategy(StopStrategies.stopAfterAttempt(12))
          .build();

  private final BillingProjectBufferService billingProjectBufferService;
  private final WorkspaceService workspaceService;
  private final WorkspaceMapper workspaceMapper;
  private final CdrVersionDao cdrVersionDao;
  private final UserDao userDao;
  private Provider<User> userProvider;
  private final FireCloudService fireCloudService;
  private final CloudStorageService cloudStorageService;
  private final Clock clock;
  private final NotebooksService notebooksService;
  private final UserService userService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public WorkspacesController(
      BillingProjectBufferService billingProjectBufferService,
      WorkspaceService workspaceService,
      WorkspaceMapper workspaceMapper,
      CdrVersionDao cdrVersionDao,
      UserDao userDao,
      Provider<User> userProvider,
      FireCloudService fireCloudService,
      CloudStorageService cloudStorageService,
      Clock clock,
      NotebooksService notebooksService,
      UserService userService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.billingProjectBufferService = billingProjectBufferService;
    this.workspaceService = workspaceService;
    this.workspaceMapper = workspaceMapper;
    this.cdrVersionDao = cdrVersionDao;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.fireCloudService = fireCloudService;
    this.cloudStorageService = cloudStorageService;
    this.clock = clock;
    this.notebooksService = notebooksService;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @VisibleForTesting
  void setWorkbenchConfigProvider(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private String getRegisteredUserDomainEmail() {
    ManagedGroupWithMembers registeredDomainGroup =
        fireCloudService.getGroup(workbenchConfigProvider.get().firecloud.registeredDomainName);
    return registeredDomainGroup.getGroupEmail();
  }

  private static String generateRandomChars(String candidateChars, int length) {
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
    }
    return sb.toString();
  }

  private CdrVersion setCdrVersionId(
      org.pmiops.workbench.db.model.Workspace dbWorkspace, String cdrVersionId) {
    if (Strings.isNullOrEmpty(cdrVersionId)) {
      throw new BadRequestException("missing cdrVersionId");
    }
    try {
      CdrVersion cdrVersion = cdrVersionDao.findOne(Long.parseLong(cdrVersionId));
      if (cdrVersion == null) {
        throw new BadRequestException(
            String.format("CDR version with ID %s not found", cdrVersionId));
      }
      dbWorkspace.setCdrVersion(cdrVersion);
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
      strippedName = generateRandomChars(RANDOM_CHARS, NUM_RANDOM_CHARS);
    }
    return new FirecloudWorkspaceId(namespace, strippedName);
  }

  private org.pmiops.workbench.firecloud.model.Workspace attemptFirecloudWorkspaceCreation(
      FirecloudWorkspaceId workspaceId) {
    fireCloudService.createWorkspace(
        workspaceId.getWorkspaceNamespace(), workspaceId.getWorkspaceName());
    return fireCloudService
        .getWorkspace(workspaceId.getWorkspaceNamespace(), workspaceId.getWorkspaceName())
        .getWorkspace();
  }

  @Override
  public ResponseEntity<Workspace> createWorkspace(Workspace workspace) {
    if (Strings.isNullOrEmpty(workspace.getName())) {
      throw new BadRequestException("missing required field 'name'");
    } else if (workspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'researchPurpose'");
    } else if (workspace.getDataAccessLevel() == null) {
      throw new BadRequestException("missing required field 'dataAccessLevel'");
    } else if (workspace.getName().length() > 80) {
      throw new BadRequestException("Workspace name must be 80 characters or less");
    }

    User user = userProvider.get();
    String workspaceNamespace;
    BillingProjectBufferEntry bufferedBillingProject;
    try {
      bufferedBillingProject = billingProjectBufferService.assignBillingProject(user);
    } catch (EmptyBufferException e) {
      throw new TooManyRequestsException();
    }
    workspaceNamespace = bufferedBillingProject.getFireCloudProjectName();

    // Note: please keep any initialization logic here in sync with CloneWorkspace().
    FirecloudWorkspaceId workspaceId =
        generateFirecloudWorkspaceId(workspaceNamespace, workspace.getName());
    FirecloudWorkspaceId fcWorkspaceId = workspaceId;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        attemptFirecloudWorkspaceCreation(fcWorkspaceId);

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        new org.pmiops.workbench.db.model.Workspace();
    dbWorkspace.setFirecloudName(fcWorkspaceId.getWorkspaceName());
    dbWorkspace.setWorkspaceNamespace(fcWorkspaceId.getWorkspaceNamespace());
    dbWorkspace.setCreator(user);
    dbWorkspace.setFirecloudUuid(fcWorkspace.getWorkspaceId());
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    setCdrVersionId(dbWorkspace, workspace.getCdrVersionId());

    org.pmiops.workbench.db.model.Workspace reqWorkspace = workspaceMapper.toDbWorkspace(workspace);
    // TODO: enforce data access level authorization
    dbWorkspace.setDataAccessLevel(reqWorkspace.getDataAccessLevel());
    dbWorkspace.setName(reqWorkspace.getName());

    // Ignore incoming fields pertaining to review status; clients can only request a review.
    workspaceMapper.setResearchPurposeDetails(dbWorkspace, workspace.getResearchPurpose());
    if (reqWorkspace.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(reqWorkspace.getReviewRequested());

    dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.NEW);

    dbWorkspace = workspaceService.getDao().save(dbWorkspace);

    return ResponseEntity.ok(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteWorkspace(
      String workspaceNamespace, String workspaceId) {
    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);
    // This automatically handles access control to the workspace.
    fireCloudService.deleteWorkspace(workspaceNamespace, workspaceId);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(workspaceService.getWorkspace(workspaceNamespace, workspaceId));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(workspaceService.getWorkspaces());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Workspace> updateWorkspace(
      String workspaceNamespace, String workspaceId, UpdateWorkspaceRequest request) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);
    workspaceService.enforceWorkspaceAccessLevel(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.OWNER);
    Workspace workspace = request.getWorkspace();
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId).getWorkspace();
    if (workspace == null) {
      throw new BadRequestException("No workspace provided in request");
    }
    if (Strings.isNullOrEmpty(workspace.getEtag())) {
      throw new BadRequestException("Missing required update field 'etag'");
    }
    int version = Etags.toVersion(workspace.getEtag());
    if (dbWorkspace.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated workspace version");
    }
    if (workspace.getDataAccessLevel() != null
        && !dbWorkspace.getDataAccessLevelEnum().equals(workspace.getDataAccessLevel())) {
      throw new BadRequestException("Attempted to change data access level");
    }
    if (workspace.getName() != null) {
      dbWorkspace.setName(workspace.getName());
    }
    ResearchPurpose researchPurpose = request.getWorkspace().getResearchPurpose();
    if (researchPurpose != null) {
      workspaceMapper.setResearchPurposeDetails(dbWorkspace, researchPurpose);
      if (researchPurpose.getReviewRequested()) {
        Timestamp now = new Timestamp(clock.instant().toEpochMilli());
        dbWorkspace.setTimeRequested(now);
      }
      dbWorkspace.setReviewRequested(researchPurpose.getReviewRequested());
    }
    // The version asserted on save is the same as the one we read via
    // getRequired() above, see RW-215 for details.
    dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace);
    return ResponseEntity.ok(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
  }

  @Override
  public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(
      String fromWorkspaceNamespace, String fromWorkspaceId, CloneWorkspaceRequest body) {
    Workspace toWorkspace = body.getWorkspace();
    if (Strings.isNullOrEmpty(toWorkspace.getName())) {
      throw new BadRequestException("missing required field 'workspace.name'");
    } else if (toWorkspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'workspace.researchPurpose'");
    }

    User user = userProvider.get();

    String toWorkspaceName;
    BillingProjectBufferEntry bufferedBillingProject;
    try {
      bufferedBillingProject = billingProjectBufferService.assignBillingProject(user);
    } catch (EmptyBufferException e) {
      throw new TooManyRequestsException();
    }
    toWorkspaceName = bufferedBillingProject.getFireCloudProjectName();

    // Retrieving the workspace is done first, which acts as an access check.
    String fromBucket =
        fireCloudService
            .getWorkspace(fromWorkspaceNamespace, fromWorkspaceId)
            .getWorkspace()
            .getBucketName();

    org.pmiops.workbench.db.model.Workspace fromWorkspace =
        workspaceService.getRequiredWithCohorts(fromWorkspaceNamespace, fromWorkspaceId);
    if (fromWorkspace == null) {
      throw new NotFoundException(
          String.format("Workspace %s/%s not found", fromWorkspaceNamespace, fromWorkspaceId));
    }

    FirecloudWorkspaceId toFcWorkspaceId =
        generateFirecloudWorkspaceId(toWorkspaceName, toWorkspace.getName());
    fireCloudService.cloneWorkspace(
        fromWorkspaceNamespace,
        fromWorkspaceId,
        toFcWorkspaceId.getWorkspaceNamespace(),
        toFcWorkspaceId.getWorkspaceName());

    org.pmiops.workbench.firecloud.model.Workspace toFcWorkspace =
        fireCloudService
            .getWorkspace(
                toFcWorkspaceId.getWorkspaceNamespace(), toFcWorkspaceId.getWorkspaceName())
            .getWorkspace();

    // In the future, we may want to allow callers to specify whether files
    // should be cloned at all (by default, yes), else they are currently stuck
    // if someone accidentally adds a large file or if there are too many to
    // feasibly copy within a single API request.
    for (Blob b : cloudStorageService.getBlobList(fromBucket)) {
      if (b.getSize() != null && b.getSize() / 1e6 > MAX_CLONE_FILE_SIZE_MB) {
        throw new FailedPreconditionException(
            String.format(
                "workspace %s/%s contains a file larger than %dMB: '%s'; cannot clone - please "
                    + "remove this file, reduce its size, or contact the workspace owner",
                fromWorkspaceNamespace, fromWorkspaceId, MAX_CLONE_FILE_SIZE_MB, b.getName()));
      }

      try {
        retryer.call(() -> copyBlob(toFcWorkspace.getBucketName(), b));
      } catch (RetryException | ExecutionException e) {
        log.log(
            Level.SEVERE,
            "Could not copy notebooks into new workspace's bucket "
                + toFcWorkspace.getBucketName());
        throw new WorkbenchException(e);
      }
    }

    // The final step in the process is to clone the AoU representation of the
    // workspace. The implication here is that we may generate orphaned
    // Firecloud workspaces / buckets, but a user should not be able to see
    // half-way cloned workspaces via AoU - so it will just appear as a
    // transient failure.
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        new org.pmiops.workbench.db.model.Workspace();

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbWorkspace.setFirecloudName(toFcWorkspaceId.getWorkspaceName());
    dbWorkspace.setWorkspaceNamespace(toFcWorkspaceId.getWorkspaceNamespace());
    dbWorkspace.setCreator(user);
    dbWorkspace.setFirecloudUuid(toFcWorkspace.getWorkspaceId());
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    dbWorkspace.setName(body.getWorkspace().getName());
    ResearchPurpose researchPurpose = body.getWorkspace().getResearchPurpose();
    workspaceMapper.setResearchPurposeDetails(dbWorkspace, researchPurpose);
    if (researchPurpose.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(researchPurpose.getReviewRequested());

    // Clone CDR version from the source, by default.
    String reqCdrVersionId = body.getWorkspace().getCdrVersionId();
    if (Strings.isNullOrEmpty(reqCdrVersionId)
        || reqCdrVersionId.equals(Long.toString(fromWorkspace.getCdrVersion().getCdrVersionId()))) {
      dbWorkspace.setCdrVersion(fromWorkspace.getCdrVersion());
      dbWorkspace.setDataAccessLevel(fromWorkspace.getDataAccessLevel());
    } else {
      CdrVersion reqCdrVersion = setCdrVersionId(dbWorkspace, reqCdrVersionId);
      dbWorkspace.setDataAccessLevelEnum(reqCdrVersion.getDataAccessLevelEnum());
    }

    dbWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.NEW);

    org.pmiops.workbench.db.model.Workspace savedWorkspace =
        workspaceService.saveAndCloneCohortsAndConceptSets(fromWorkspace, dbWorkspace);

    if (Optional.ofNullable(body.getIncludeUserRoles()).orElse(false)) {
      Map<String, WorkspaceAccessEntry> fromAclsMap =
          workspaceService.getFirecloudWorkspaceAcls(
              fromWorkspace.getWorkspaceNamespace(), fromWorkspace.getFirecloudName());

      Map<String, WorkspaceAccessLevel> clonedRoles = new HashMap<>();
      for (Map.Entry<String, WorkspaceAccessEntry> entry : fromAclsMap.entrySet()) {
        if (!entry.getKey().equals(user.getEmail())) {
          clonedRoles.put(
              entry.getKey(), WorkspaceAccessLevel.fromValue(entry.getValue().getAccessLevel()));
        } else {
          clonedRoles.put(entry.getKey(), WorkspaceAccessLevel.OWNER);
        }
      }
      savedWorkspace =
          workspaceService.updateWorkspaceAcls(
              savedWorkspace, clonedRoles, getRegisteredUserDomainEmail());
    }
    return ResponseEntity.ok(
        new CloneWorkspaceResponse()
            .workspace(workspaceMapper.toApiWorkspace(savedWorkspace, toFcWorkspace)));
  }

  // A retry period is needed because the permission to copy files into the cloned workspace is not
  // granted transactionally
  private Boolean copyBlob(String bucketName, Blob b) {
    try {
      cloudStorageService.copyBlob(b.getBlobId(), BlobId.of(bucketName, b.getName()));
      return true;
    } catch (StorageException e) {
      log.warning("Service Account does not have access to bucket " + bucketName);
      throw e;
    }
  }

  @Override
  public ResponseEntity<WorkspaceUserRolesResponse> shareWorkspace(
      String workspaceNamespace, String workspaceId, ShareWorkspaceRequest request) {
    if (Strings.isNullOrEmpty(request.getWorkspaceEtag())) {
      throw new BadRequestException("Missing required update field 'workspaceEtag'");
    }

    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);
    int version = Etags.toVersion(request.getWorkspaceEtag());
    if (dbWorkspace.getVersion() != version) {
      throw new ConflictException("Attempted to modify user roles with outdated workspace etag");
    }
    Map<String, WorkspaceAccessLevel> shareRolesMap = new HashMap<>();
    for (UserRole role : request.getItems()) {
      if (role.getRole() == null || role.getRole().toString().trim().isEmpty()) {
        throw new BadRequestException("Role required.");
      }
      User newUser = userDao.findUserByEmail(role.getEmail());
      if (newUser == null) {
        throw new BadRequestException(String.format("User %s doesn't exist", role.getEmail()));
      }
      shareRolesMap.put(role.getEmail(), role.getRole());
    }
    // This automatically enforces the "canShare" permission.
    dbWorkspace =
        workspaceService.updateWorkspaceAcls(
            dbWorkspace, shareRolesMap, getRegisteredUserDomainEmail());
    WorkspaceUserRolesResponse resp = new WorkspaceUserRolesResponse();
    resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.getVersion()));

    Map<String, WorkspaceAccessEntry> updatedWsAcls =
        workspaceService.getFirecloudWorkspaceAcls(
            workspaceNamespace, dbWorkspace.getFirecloudName());
    List<UserRole> updatedUserRoles =
        workspaceService.convertWorkspaceAclsToUserRoles(updatedWsAcls);
    resp.setItems(updatedUserRoles);
    return ResponseEntity.ok(resp);
  }

  /** Record approval or rejection of research purpose. */
  @Override
  @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
  public ResponseEntity<EmptyResponse> reviewWorkspace(
      String ns, String id, ResearchPurposeReviewRequest review) {
    org.pmiops.workbench.db.model.Workspace workspace = workspaceService.get(ns, id);
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
  // We can add pagination in the DAO by returning Slice<Workspace> if we want the method to return
  // pagination information (e.g. are there more workspaces to get), and Page<Workspace> if we
  // want the method to return both pagination information and a total count.
  @Override
  @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
  public ResponseEntity<WorkspaceListResponse> getWorkspacesForReview() {
    WorkspaceListResponse response = new WorkspaceListResponse();
    List<org.pmiops.workbench.db.model.Workspace> workspaces = workspaceService.findForReview();
    response.setItems(
        workspaces.stream().map(workspaceMapper::toApiWorkspace).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(
      String workspaceNamespace, String workspaceId) {
    List<FileDetail> fileList = notebooksService.getNotebooks(workspaceNamespace, workspaceId);
    return ResponseEntity.ok(fileList);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteNotebook(
      String workspace, String workspaceName, String notebookName) {
    notebooksService.deleteNotebook(workspace, workspaceName, notebookName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyNotebook(
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

    return ResponseEntity.ok(fileDetail);
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
        cloudStorageService.getMetadata(bucketName, "notebooks/" + notebookName);

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
            workspaceService.getFirecloudWorkspaceAcls(workspaceNamespace, workspaceName).keySet();

        response.lastLockedBy(findHashedUser(bucketName, workspaceUsers, lastLockedByHash));
      }
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
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);

    Map<String, WorkspaceAccessEntry> firecloudAcls =
        workspaceService.getFirecloudWorkspaceAcls(
            workspaceNamespace, dbWorkspace.getFirecloudName());
    List<UserRole> userRoles = workspaceService.convertWorkspaceAclsToUserRoles(firecloudAcls);
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
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);

    workspaceService.setPublished(dbWorkspace, getRegisteredUserDomainEmail(), true);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.FEATURED_WORKSPACE_ADMIN})
  public ResponseEntity<EmptyResponse> unpublishWorkspace(
      String workspaceNamespace, String workspaceId) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);

    workspaceService.setPublished(dbWorkspace, getRegisteredUserDomainEmail(), false);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<RecentWorkspaceResponse> getUserRecentWorkspaces() {
    long userId = userProvider.get().getUserId();
    List<UserRecentWorkspace> userRecentWorkspaceList =
            workspaceService.getRecentWorkspacesByUser(userId);
    RecentWorkspaceResponse recentWorkspaceResponse = new RecentWorkspaceResponse();
    recentWorkspaceResponse.addAll(
            userRecentWorkspaceList.stream()
                    .map(userRecentWorkspace -> {
                      RecentWorkspace recentWorkspace = new RecentWorkspace();
                      recentWorkspace.setWorkspaceId(userRecentWorkspace.getWorkspaceId());
                      recentWorkspace.setModifiedTime(userRecentWorkspace.getLastAccessDate().toString());
                      return recentWorkspace;
                    })
                    .collect(Collectors.toList())
    );
    return ResponseEntity.ok(recentWorkspaceResponse);
  }
}
