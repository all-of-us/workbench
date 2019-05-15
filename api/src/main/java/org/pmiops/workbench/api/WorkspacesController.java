package org.pmiops.workbench.api;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.EmptyBufferException;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.BillingProjectBufferEntry;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.exceptions.*;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.notebooks.BlobAlreadyExistsException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.workspaces.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class WorkspacesController implements WorkspacesApiDelegate {

  private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());

  private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyz";
  private static final int NUM_RANDOM_CHARS = 20;
  private static final int MAX_FC_CREATION_ATTEMPT_VALUES = 6;
  // If we later decide to tune this value, consider moving to the WorkbenchConfig.
  private static final int MAX_NOTEBOOK_SIZE_MB = 100;
  // "directory" for notebooks, within the workspace cloud storage bucket.
  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = NotebooksService.NOTEBOOKS_WORKSPACE_DIRECTORY;
  private static final Pattern NOTEBOOK_PATTERN = NotebooksService.NOTEBOOK_PATTERN;

  private final BillingProjectBufferService billingProjectBufferService;
  private final WorkspaceService workspaceService;
  private final WorkspaceMapper workspaceMapper;
  private final CdrVersionDao cdrVersionDao;
  private final CohortDao cohortDao;
  private final CohortFactory cohortFactory;
  private final ConceptSetDao conceptSetDao;
  private final UserDao userDao;
  private Provider<User> userProvider;
  private final FireCloudService fireCloudService;
  private final CloudStorageService cloudStorageService;
  private final Clock clock;
  private final NotebooksService notebooksService;
  private final UserService userService;
  private final Provider<WorkbenchConfig> workbenchConfig;

  @Autowired
  WorkspacesController(
      BillingProjectBufferService billingProjectBufferService,
      WorkspaceService workspaceService,
      WorkspaceMapper workspaceMapper,
      CdrVersionDao cdrVersionDao,
      CohortDao cohortDao,
      CohortFactory cohortFactory,
      ConceptSetDao conceptSetDao,
      UserDao userDao,
      Provider<User> userProvider,
      FireCloudService fireCloudService,
      CloudStorageService cloudStorageService,
      Clock clock,
      NotebooksService notebooksService,
      UserService userService,
      Provider<WorkbenchConfig> workbenchConfig) {
    this.billingProjectBufferService = billingProjectBufferService;
    this.workspaceService = workspaceService;
    this.workspaceMapper = workspaceMapper;
    this.cdrVersionDao = cdrVersionDao;
    this.cohortDao = cohortDao;
    this.cohortFactory = cohortFactory;
    this.conceptSetDao = conceptSetDao;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.fireCloudService = fireCloudService;
    this.cloudStorageService = cloudStorageService;
    this.clock = clock;
    this.notebooksService = notebooksService;
    this.userService = userService;
    this.workbenchConfig = workbenchConfig;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  private static String generateRandomChars(String candidateChars, int length) {
    StringBuilder sb = new StringBuilder();
    Random random = new Random();
    for (int i = 0; i < length; i++) {
      sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
    }
    return sb.toString();
  }

  private CdrVersion setCdrVersionId(org.pmiops.workbench.db.model.Workspace dbWorkspace,
      String cdrVersionId) {
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
    fireCloudService.createWorkspace(workspaceId.getWorkspaceNamespace(),
        workspaceId.getWorkspaceName());
    return fireCloudService.getWorkspace(workspaceId.getWorkspaceNamespace(),
        workspaceId.getWorkspaceName()).getWorkspace();
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

    final boolean useBillingProjectBuffer = workbenchConfig.get().featureFlags.useBillingProjectBuffer;
    if (useBillingProjectBuffer) {
      if (Strings.isNullOrEmpty(workspace.getNamespace())) {
        throw new BadRequestException("missing required field 'namespace'");
      }
      org.pmiops.workbench.db.model.Workspace existingWorkspace = workspaceService.getByName(
              workspace.getNamespace(), workspace.getName());
      if (existingWorkspace != null) {
        throw new ConflictException(String.format(
                "Workspace %s/%s already exists",
                workspace.getNamespace(), workspace.getName()));
      }
    }

    User user = userProvider.get();
    String workspaceNamespace;
    if (useBillingProjectBuffer) {
      BillingProjectBufferEntry bufferedBillingProject;
      try {
        bufferedBillingProject = billingProjectBufferService.assignBillingProject(user);
      } catch (EmptyBufferException e) {
        throw new TooManyRequestsException();
      }
      workspaceNamespace = bufferedBillingProject.getFireCloudProjectName();
    } else {
      workspaceNamespace = workspace.getNamespace();
    }

    // Note: please keep any initialization logic here in sync with CloneWorkspace().
    FirecloudWorkspaceId workspaceId = generateFirecloudWorkspaceId(workspaceNamespace,
        workspace.getName());
    FirecloudWorkspaceId fcWorkspaceId = workspaceId;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;

    if (useBillingProjectBuffer) {
      fcWorkspace = attemptFirecloudWorkspaceCreation(fcWorkspaceId);
    } else {
      fcWorkspace = null;
      for (int attemptValue = 1; attemptValue < MAX_FC_CREATION_ATTEMPT_VALUES; attemptValue++) {
        try {
          fcWorkspace = attemptFirecloudWorkspaceCreation(fcWorkspaceId);
          break;
        } catch (ConflictException e) {
          if (attemptValue >= 5) {
            throw e;
          } else {
            fcWorkspaceId =
                    new FirecloudWorkspaceId(workspaceId.getWorkspaceNamespace(),
                            workspaceId.getWorkspaceName() + Integer.toString(attemptValue));
          }
        }
      }
    }

    cloudStorageService.copyAllDemoNotebooks(fcWorkspace.getBucketName());

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
    dbWorkspace.setDescription(reqWorkspace.getDescription());

    // Ignore incoming fields pertaining to review status; clients can only request a review.
    workspaceMapper.setResearchPurposeDetails(dbWorkspace, workspace.getResearchPurpose());
    if (reqWorkspace.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(reqWorkspace.getReviewRequested());

    WorkspaceUserRole permissions = new WorkspaceUserRole();
    permissions.setRoleEnum(WorkspaceAccessLevel.OWNER);
    permissions.setWorkspace(dbWorkspace);
    permissions.setUser(user);

    dbWorkspace.addWorkspaceUserRole(permissions);
    dbWorkspace = workspaceService.getDao().save(dbWorkspace);

    org.pmiops.workbench.db.model.Workspace finalDbWorkspace = dbWorkspace;
    cloudStorageService.readAllDemoCohorts().stream()
        .map(apiCohort -> cohortFactory
            .createCohort(apiCohort, userProvider.get(), finalDbWorkspace.getWorkspaceId())
        ).forEach(dbCohort -> {
      try {
        dbCohort = cohortDao.save(dbCohort);
      } catch (DataIntegrityViolationException e) {
        throw new BadRequestException(String.format(
            "Cohort \"/%s/%s/%d\" already exists.",
            finalDbWorkspace.getWorkspaceNamespace(), finalDbWorkspace.getWorkspaceId(),
            dbCohort.getCohortId()));
      }
    });

    List<JSONObject> demoConceptSets = cloudStorageService.readAllDemoConceptSets();
    for (JSONObject conceptSet : demoConceptSets) {
      ConceptSet dbConceptSet = new ConceptSet();

      dbConceptSet.setName(conceptSet.getString("name"));
      dbConceptSet.setDescription(conceptSet.getString("description"));
      dbConceptSet.setCreator(userProvider.get());
      dbConceptSet.setWorkspaceId(dbWorkspace.getWorkspaceId());
      dbConceptSet.setCreationTime(now);
      dbConceptSet.setLastModifiedTime(now);
      dbConceptSet.setVersion(1);
      dbConceptSet.setParticipantCount(conceptSet.getInt("participant_count"));
      dbConceptSet.setDomain(
          CommonStorageEnums.domainToStorage(Domain.fromValue(conceptSet.getString("domain"))));
      try {
        List<Object> conceptIdsJSON = conceptSet.getJSONArray("concept_ids").toList();
        Set<Long> conceptIds = conceptIdsJSON
            .stream()
            .map(Object::toString)
            .map(Long::valueOf)
            .collect(Collectors.toSet());
        dbConceptSet.getConceptIds().addAll(conceptIds);
      } catch (JSONException e) {
        throw new ServerErrorException(String.format(
            "concept_ids cannot be read from %s", conceptSet.getString("name")
        ));
      }

      try {
        dbConceptSet = conceptSetDao.save(dbConceptSet);
      } catch (DataIntegrityViolationException e) {
        throw new ServerErrorException(String.format(
            "Concept Set \"/%s/%s/%d\" already exists.",
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getWorkspaceId(),
            dbConceptSet.getConceptSetId()));
      }
    }
    return ResponseEntity.ok(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteWorkspace(String workspaceNamespace,
      String workspaceId) {
    // This deletes all Firecloud and google resources, however saves all references
    // to the workspace and its resources in the Workbench database.
    // This is for auditing purposes and potentially workspace restore.
    // TODO: do we want to delete workspace resource references and save only metadata?
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);
    // This automatically handles access control to the workspace.
    fireCloudService.deleteWorkspace(workspaceNamespace, workspaceId);
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(String workspaceNamespace,
      String workspaceId) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);

    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;

    WorkspaceResponse response = new WorkspaceResponse();

    // This enforces access controls.
    fcResponse = fireCloudService.getWorkspace(
        workspaceNamespace, workspaceId);
    fcWorkspace = fcResponse.getWorkspace();

    if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      // We don't expose PROJECT_OWNER in our API; just use OWNER.
      response.setAccessLevel(WorkspaceAccessLevel.OWNER);
    } else {
      response.setAccessLevel(WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel()));
      if (response.getAccessLevel() == null) {
        throw new ServerErrorException("Unsupported access level: " + fcResponse.getAccessLevel());
      }
    }
    response.setWorkspace(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(workspaceService.getWorkspaces());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Workspace> updateWorkspace(String workspaceNamespace, String workspaceId,
      UpdateWorkspaceRequest request) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.OWNER);
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
    if (workspace.getDataAccessLevel() != null &&
        !dbWorkspace.getDataAccessLevelEnum().equals(workspace.getDataAccessLevel())) {
      throw new BadRequestException("Attempted to change data access level");
    }
    if (workspace.getDescription() != null) {
      dbWorkspace.setDescription(workspace.getDescription());
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
  public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(String fromWorkspaceNamespace,
      String fromWorkspaceId, CloneWorkspaceRequest body) {
    Workspace toWorkspace = body.getWorkspace();
    if (Strings.isNullOrEmpty(toWorkspace.getName())) {
      throw new BadRequestException("missing required field 'workspace.name'");
    } else if (toWorkspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'workspace.researchPurpose'");
    }

    final boolean useBillingProjectBuffer = workbenchConfig.get().featureFlags.useBillingProjectBuffer;
    if (!useBillingProjectBuffer) {
      if (Strings.isNullOrEmpty(toWorkspace.getNamespace())) {
        throw new BadRequestException("missing required field 'workspace.namespace'");
      }
      if (workspaceService.getByName(toWorkspace.getNamespace(), toWorkspace.getName()) != null) {
        throw new ConflictException(String.format(
                "Workspace %s/%s already exists",
                toWorkspace.getNamespace(), toWorkspace.getName()));
      }
    }

    User user = userProvider.get();

    String toWorkspaceName;
    if (useBillingProjectBuffer) {
      BillingProjectBufferEntry bufferedBillingProject;
      try {
        bufferedBillingProject = billingProjectBufferService.assignBillingProject(user);
      } catch (EmptyBufferException e) {
        throw new TooManyRequestsException();
      }
      toWorkspaceName = bufferedBillingProject.getFireCloudProjectName();
    } else {
      toWorkspaceName = toWorkspace.getNamespace();

    }

    // Retrieving the workspace is done first, which acts as an access check.
    String fromBucket = fireCloudService.getWorkspace(fromWorkspaceNamespace, fromWorkspaceId)
        .getWorkspace()
        .getBucketName();

    org.pmiops.workbench.db.model.Workspace fromWorkspace =
        workspaceService.getRequiredWithCohorts(fromWorkspaceNamespace, fromWorkspaceId);
    if (fromWorkspace == null) {
      throw new NotFoundException(String.format(
          "Workspace %s/%s not found", fromWorkspaceNamespace, fromWorkspaceId));
    }

    FirecloudWorkspaceId toFcWorkspaceId = generateFirecloudWorkspaceId(toWorkspaceName,
        toWorkspace.getName());
    fireCloudService.cloneWorkspace(fromWorkspaceNamespace, fromWorkspaceId,
        toFcWorkspaceId.getWorkspaceNamespace(), toFcWorkspaceId.getWorkspaceName());

    org.pmiops.workbench.firecloud.model.Workspace toFcWorkspace =
        fireCloudService.getWorkspace(toFcWorkspaceId.getWorkspaceNamespace(),
            toFcWorkspaceId.getWorkspaceName()).getWorkspace();

    // In the future, we may want to allow callers to specify whether notebooks
    // should be cloned at all (by default, yes), else they are currently stuck
    // if someone accidentally adds a large notebook or if there are too many to
    // feasibly copy within a single API request.
    for (Blob b : cloudStorageService.getBlobList(fromBucket, NOTEBOOKS_WORKSPACE_DIRECTORY)) {
      if (!NOTEBOOK_PATTERN.matcher(b.getName()).matches()) {
        continue;
      }
      if (b.getSize() != null && b.getSize() / 1e6 > MAX_NOTEBOOK_SIZE_MB) {
        throw new FailedPreconditionException(String.format(
            "workspace %s/%s contains a notebook larger than %dMB: '%s'; cannot clone - please " +
                "remove this notebook, reduce its size, or contact the workspace owner",
            fromWorkspaceNamespace, fromWorkspaceId, MAX_NOTEBOOK_SIZE_MB, b.getName()));
      }
      cloudStorageService.copyBlob(
          b.getBlobId(), BlobId.of(toFcWorkspace.getBucketName(), b.getName()));
    }

    // The final step in the process is to clone the AoU representation of the
    // workspace. The implication here is that we may generate orphaned
    // Firecloud workspaces / buckets, but a user should not be able to see
    // half-way cloned workspaces via AoU - so it will just appear as a
    // transient failure.
    org.pmiops.workbench.db.model.Workspace toDbWorkspace =
        workspaceMapper.toDbWorkspace(body.getWorkspace());
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

    dbWorkspace.setName(toDbWorkspace.getName());
    ResearchPurpose researchPurpose = body.getWorkspace().getResearchPurpose();
    workspaceMapper.setResearchPurposeDetails(dbWorkspace, researchPurpose);
    if (researchPurpose.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(researchPurpose.getReviewRequested());

    // Clone description/CDR version from the source, by default.
    if (Strings.isNullOrEmpty(toDbWorkspace.getDescription())) {
      dbWorkspace.setDescription(fromWorkspace.getDescription());
    } else {
      dbWorkspace.setDescription(toDbWorkspace.getDescription());
    }
    String reqCdrVersionId = body.getWorkspace().getCdrVersionId();
    if (Strings.isNullOrEmpty(reqCdrVersionId) ||
        reqCdrVersionId.equals(Long.toString(fromWorkspace.getCdrVersion().getCdrVersionId()))) {
      dbWorkspace.setCdrVersion(fromWorkspace.getCdrVersion());
      dbWorkspace.setDataAccessLevel(fromWorkspace.getDataAccessLevel());
    } else {
      CdrVersion reqCdrVersion = setCdrVersionId(dbWorkspace, reqCdrVersionId);
      dbWorkspace.setDataAccessLevelEnum(reqCdrVersion.getDataAccessLevelEnum());
    }

    WorkspaceUserRole ownerRole = new WorkspaceUserRole();
    ownerRole.setRoleEnum(WorkspaceAccessLevel.OWNER);
    ownerRole.setWorkspace(dbWorkspace);
    ownerRole.setUser(user);

    dbWorkspace.addWorkspaceUserRole(ownerRole);
    org.pmiops.workbench.db.model.Workspace savedWorkspace =
        workspaceService.saveAndCloneCohortsAndConceptSets(fromWorkspace, dbWorkspace);

    if (Optional.ofNullable(body.getIncludeUserRoles()).orElse(false)) {
      Set<WorkspaceUserRole> clonedRoles = fromWorkspace.getWorkspaceUserRoles().stream()
          .filter((role) -> role.getUser().getUserId() != user.getUserId())
          .map((role) -> {
            WorkspaceUserRole to = new WorkspaceUserRole();
            to.setUser(role.getUser());
            to.setWorkspace(dbWorkspace);
            to.setRole(role.getRole());
            return to;
          })
          .collect(Collectors.toSet());
      clonedRoles.add(ownerRole);
      savedWorkspace = workspaceService.updateUserRoles(savedWorkspace, clonedRoles);
    }
    return ResponseEntity.ok(new CloneWorkspaceResponse().workspace(workspaceMapper.toApiWorkspace(savedWorkspace, toFcWorkspace)));
  }

  @Override
  public ResponseEntity<ShareWorkspaceResponse> shareWorkspace(String workspaceNamespace,
      String workspaceId,
      ShareWorkspaceRequest request) {
    if (Strings.isNullOrEmpty(request.getWorkspaceEtag())) {
      throw new BadRequestException("Missing required update field 'workspaceEtag'");
    }

    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceService.getRequired(workspaceNamespace, workspaceId);
    int version = Etags.toVersion(request.getWorkspaceEtag());
    if (dbWorkspace.getVersion() != version) {
      throw new ConflictException("Attempted to modify user roles with outdated workspace etag");
    }
    Set<WorkspaceUserRole> dbUserRoles = new HashSet<WorkspaceUserRole>();
    for (UserRole role : request.getItems()) {
      if (role.getRole() == null || role.getRole().toString().trim().isEmpty()) {
        throw new BadRequestException("Role required.");
      }
      WorkspaceUserRole newUserRole = new WorkspaceUserRole();
      User newUser = userDao.findUserByEmail(role.getEmail());
      if (newUser == null) {
        throw new BadRequestException(String.format(
            "User %s doesn't exist",
            role.getEmail()));
      }
      newUserRole.setUser(newUser);
      newUserRole.setRoleEnum(role.getRole());
      dbUserRoles.add(newUserRole);
    }
    // This automatically enforces owner role.
    dbWorkspace = workspaceService.updateUserRoles(dbWorkspace, dbUserRoles);
    ShareWorkspaceResponse resp = new ShareWorkspaceResponse();
    resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.getVersion()));
    List<UserRole> updatedUserRoles = dbWorkspace.getWorkspaceUserRoles()
        .stream()
        .map(r -> new UserRole()
            .email(r.getUser().getEmail())
            .givenName(r.getUser().getGivenName())
            .familyName(r.getUser().getFamilyName())
            .role(r.getRoleEnum()))
        // Reverse sorting arranges the role list in a logical order - owners first, then by email.
        .sorted(
            Comparator.comparing(UserRole::getRole).thenComparing(UserRole::getEmail).reversed())
        .collect(Collectors.toList());
    resp.setItems(updatedUserRoles);
    return ResponseEntity.ok(resp);
  }

  /**
   * Record approval or rejection of research purpose.
   */
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
    response.setItems(workspaces.stream().map(workspaceMapper::toApiWorkspace).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(String workspaceNamespace,
      String workspaceId) {
    List<FileDetail> fileList = notebooksService.getNotebooks(workspaceNamespace, workspaceId);
    return ResponseEntity.ok(fileList);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteNotebook(String workspace, String workspaceName,
      String notebookName) {
    notebooksService.deleteNotebook(workspace, workspaceName, notebookName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<FileDetail> copyNotebook(String fromWorkspaceNamespace, String fromWorkspaceId,
      String fromNotebookName, CopyNotebookRequest copyNotebookRequest) {
    FileDetail fileDetail;
    try {
      fileDetail = notebooksService.copyNotebook(fromWorkspaceNamespace, fromWorkspaceId,
          fromNotebookName,
          copyNotebookRequest.getToWorkspaceNamespace(), copyNotebookRequest.getToWorkspaceName(),
          copyNotebookRequest.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<FileDetail> cloneNotebook(String workspace, String workspaceName,
      String notebookName) {
    FileDetail fileDetail;
    try {
      fileDetail = notebooksService.cloneNotebook(workspace, workspaceName, notebookName);
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }

  @Override
  public ResponseEntity<FileDetail> renameNotebook(String workspace, String workspaceName,
      NotebookRename rename) {
    FileDetail fileDetail;
    try {
      fileDetail = notebooksService.renameNotebook(workspace, workspaceName, rename.getName(), rename.getNewName());
    } catch (BlobAlreadyExistsException e) {
      throw new BadRequestException("File already exists at copy destination");
    }

    return ResponseEntity.ok(fileDetail);
  }
}
