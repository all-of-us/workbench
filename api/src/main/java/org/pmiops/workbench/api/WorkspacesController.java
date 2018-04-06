package org.pmiops.workbench.api;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResearchPurposeReviewRequest;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.ShareWorkspaceResponse;
import org.pmiops.workbench.model.UnderservedPopulationEnum;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceListResponse;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final Pattern NOTEBOOK_PATTERN = Pattern.compile("([^\\s]+(\\.(?i)(ipynb))$)");
  // "directory" for notebooks, within the workspace cloud storage bucket.
  private static final String NOTEBOOKS_WORKSPACE_DIRECTORY = "notebooks";

  private final WorkspaceService workspaceService;
  private final CdrVersionDao cdrVersionDao;
  private final UserDao userDao;
  private Provider<User> userProvider;
  private final FireCloudService fireCloudService;
  private final CloudStorageService cloudStorageService;
  private final Clock clock;
  private final UserService userService;

  @Autowired
  WorkspacesController(
      WorkspaceService workspaceService,
      CdrVersionDao cdrVersionDao,
      UserDao userDao,
      Provider<User> userProvider,
      FireCloudService fireCloudService,
      CloudStorageService cloudStorageService,
      Clock clock,
      UserService userService) {
    this.workspaceService = workspaceService;
    this.cdrVersionDao = cdrVersionDao;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.fireCloudService = fireCloudService;
    this.cloudStorageService = cloudStorageService;
    this.clock = clock;
    this.userService = userService;
  }

  // This does not populate the list of underserved research groups.
  private static final Workspace constructListWorkspaceFromDb(org.pmiops.workbench.db.model.Workspace workspace,
      ResearchPurpose researchPurpose) {
    FirecloudWorkspaceId workspaceId = workspace.getFirecloudWorkspaceId();
    Workspace result = new Workspace()
        .etag(Etags.fromVersion(workspace.getVersion()))
        .lastModifiedTime(workspace.getLastModifiedTime().getTime())
        .creationTime(workspace.getCreationTime().getTime())
        .dataAccessLevel(workspace.getDataAccessLevel())
        .name(workspace.getName())
        .id(workspaceId.getWorkspaceName())
        .namespace(workspaceId.getWorkspaceNamespace())
        .description(workspace.getDescription())
        .researchPurpose(researchPurpose);
    if (workspace.getCreator() != null) {
      result.setCreator(workspace.getCreator().getEmail());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }


    result.setUserRoles(workspace.getWorkspaceUserRoles().stream().map(TO_CLIENT_USER_ROLE).collect(Collectors.toList()));
    return result;
  }

  /**
   * Converter function from backend representation (used with Hibernate) to
   * client representation (generated by Swagger).
   */
  @Deprecated
  private static final Function<org.pmiops.workbench.db.model.Workspace, Workspace>
      TO_SINGLE_CLIENT_WORKSPACE =
      new Function<org.pmiops.workbench.db.model.Workspace, Workspace>() {
        @Override
        public Workspace apply(org.pmiops.workbench.db.model.Workspace workspace) {
          ResearchPurpose researchPurpose = createResearchPurpose(workspace);


          if(workspace.getContainsUnderservedPopulation()) {
            Set<UnderservedPopulationEnum> dbSet = workspace.getUnderservedPopulationSet();
            List<UnderservedPopulationEnum> clientList = new ArrayList<UnderservedPopulationEnum>();
            for (UnderservedPopulationEnum population : dbSet) {
              clientList.add(population);
            }
            researchPurpose.setUnderservedPopulationDetails(clientList);
          }
          Workspace result = constructListWorkspaceFromDb(workspace, researchPurpose);

          return result;
        }
      };

  @Deprecated
  private static final Function<org.pmiops.workbench.db.model.Workspace, Workspace>
      TO_CLIENT_WORKSPACE =
      new Function<org.pmiops.workbench.db.model.Workspace, Workspace>() {
        @Override
        public Workspace apply(org.pmiops.workbench.db.model.Workspace workspace) {
          ResearchPurpose researchPurpose = createResearchPurpose(workspace);
          Workspace result = constructListWorkspaceFromDb(workspace, researchPurpose);
          return result;
        }
      };

  private static final ResearchPurpose createResearchPurpose(org.pmiops.workbench.db.model.Workspace workspace) {
    ResearchPurpose researchPurpose = new ResearchPurpose()
        .diseaseFocusedResearch(workspace.getDiseaseFocusedResearch())
        .diseaseOfFocus(workspace.getDiseaseOfFocus())
        .methodsDevelopment(workspace.getMethodsDevelopment())
        .controlSet(workspace.getControlSet())
        .aggregateAnalysis(workspace.getAggregateAnalysis())
        .ancestry(workspace.getAncestry())
        .commercialPurpose(workspace.getCommercialPurpose())
        .population(workspace.getPopulation())
        .populationOfFocus(workspace.getPopulationOfFocus())
        .additionalNotes(workspace.getAdditionalNotes())
        .reviewRequested(workspace.getReviewRequested())
        .approved(workspace.getApproved())
        .containsUnderservedPopulation(workspace.getContainsUnderservedPopulation());
    if(workspace.getTimeRequested() != null){
      researchPurpose.timeRequested(workspace.getTimeRequested().getTime());
    }
    return researchPurpose;
  }

  // This does not populate the list of underserved research groups.
  private static final Workspace constructListWorkspaceFromFCAndDb(org.pmiops.workbench.db.model.Workspace workspace,
      org.pmiops.workbench.firecloud.model.Workspace fcWorkspace, ResearchPurpose researchPurpose) {
    Workspace result = new Workspace()
        .etag(Etags.fromVersion(workspace.getVersion()))
        .lastModifiedTime(workspace.getLastModifiedTime().getTime())
        .creationTime(workspace.getCreationTime().getTime())
        .dataAccessLevel(workspace.getDataAccessLevel())
        .name(workspace.getName())
        .id(fcWorkspace.getName())
        .namespace(fcWorkspace.getNamespace())
        .description(workspace.getDescription())
        .researchPurpose(researchPurpose);
    if (fcWorkspace.getCreatedBy() != null) {
      result.setCreator(fcWorkspace.getCreatedBy());
    }
    if (workspace.getCdrVersion() != null) {
      result.setCdrVersionId(String.valueOf(workspace.getCdrVersion().getCdrVersionId()));
    }


    result.setUserRoles(workspace.getWorkspaceUserRoles().stream().map(TO_CLIENT_USER_ROLE).collect(Collectors.toList()));

    return result;
  }

  private static final BiFunction<org.pmiops.workbench.db.model.Workspace, org.pmiops.workbench.firecloud.model.Workspace, Workspace>
      TO_SINGLE_CLIENT_WORKSPACE_FROM_FC_AND_DB =
      new BiFunction<org.pmiops.workbench.db.model.Workspace, org.pmiops.workbench.firecloud.model.Workspace, Workspace>() {
        @Override
        public Workspace apply(org.pmiops.workbench.db.model.Workspace workspace,
            org.pmiops.workbench.firecloud.model.Workspace fcWorkspace) {
          ResearchPurpose researchPurpose = createResearchPurpose(workspace);
          if(workspace.getContainsUnderservedPopulation()) {
            Set<UnderservedPopulationEnum> dbSet = workspace.getUnderservedPopulationSet();
            List<UnderservedPopulationEnum> clientList = new ArrayList<UnderservedPopulationEnum>();
            for (UnderservedPopulationEnum population : dbSet) {
              clientList.add(population);
            }
            researchPurpose.setUnderservedPopulationDetails(clientList);
          }
          Workspace result = constructListWorkspaceFromFCAndDb(workspace, fcWorkspace, researchPurpose);
          return result;
        }
      };

  private static final Function<Workspace, org.pmiops.workbench.db.model.Workspace>
      FROM_CLIENT_WORKSPACE =
      new Function<Workspace, org.pmiops.workbench.db.model.Workspace>() {
        @Override
        public org.pmiops.workbench.db.model.Workspace apply(Workspace workspace) {
          org.pmiops.workbench.db.model.Workspace result = new org.pmiops.workbench.db.model.Workspace();
          if (workspace.getDataAccessLevel() != null) {
            result.setDataAccessLevel(
                DataAccessLevel.fromValue(workspace.getDataAccessLevel().toString()));
          }
          result.setDescription(workspace.getDescription());
          result.setName(workspace.getName());
          if (workspace.getResearchPurpose() != null) {
            setResearchPurposeDetails(result, workspace.getResearchPurpose());
            result.setReviewRequested(workspace.getResearchPurpose().getReviewRequested());
            if (workspace.getResearchPurpose().getTimeRequested() != null) {
              result.setTimeRequested(
                  new Timestamp(workspace.getResearchPurpose().getTimeRequested()));
            }
            result.setApproved(workspace.getResearchPurpose().getApproved());
          }
          return result;
        }
      };


      private static final Function<org.pmiops.workbench.db.model.WorkspaceUserRole, UserRole>
          TO_CLIENT_USER_ROLE =
          new Function<org.pmiops.workbench.db.model.WorkspaceUserRole, UserRole>() {
            @Override
            public UserRole apply(org.pmiops.workbench.db.model.WorkspaceUserRole workspaceUserRole) {
              UserRole result = new UserRole();
              result.setEmail(workspaceUserRole.getUser().getEmail());
              result.setRole(workspaceUserRole.getRole());

              return result;
            }
          };

  @VisibleForTesting
  void setUserProvider(Provider<User> userProvider) {
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

  private void setCdrVersionId(org.pmiops.workbench.db.model.Workspace dbWorkspace, String cdrVersionId) {
    if (cdrVersionId != null) {
      try {
        CdrVersion cdrVersion = cdrVersionDao.findOne(Long.parseLong(cdrVersionId));
        if (cdrVersion == null) {
          throw new BadRequestException(
              String.format("CDR version with ID %s not found", cdrVersionId));
        }
        dbWorkspace.setCdrVersion(cdrVersion);
      } catch (NumberFormatException e) {
        throw new BadRequestException(String.format(
            "Invalid cdr version ID: %s", cdrVersionId));
      }
    }
  }

  /**
   * Sets user-editable research purpose detail fields.
   */
  private static void setResearchPurposeDetails(org.pmiops.workbench.db.model.Workspace dbWorkspace,
      ResearchPurpose purpose) {
    dbWorkspace.setDiseaseFocusedResearch(purpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(purpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(purpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(purpose.getControlSet());
    dbWorkspace.setAggregateAnalysis(purpose.getAggregateAnalysis());
    dbWorkspace.setAncestry(purpose.getAncestry());
    dbWorkspace.setCommercialPurpose(purpose.getCommercialPurpose());
    dbWorkspace.setPopulation(purpose.getPopulation());
    dbWorkspace.setPopulationOfFocus(purpose.getPopulationOfFocus());
    dbWorkspace.setAdditionalNotes(purpose.getAdditionalNotes());
    dbWorkspace.setContainsUnderservedPopulation(purpose.getContainsUnderservedPopulation());
    if (purpose.getContainsUnderservedPopulation()) {
      List<UnderservedPopulationEnum> list = purpose.getUnderservedPopulationDetails();
      Set<UnderservedPopulationEnum> dbSet = new HashSet<UnderservedPopulationEnum>();
      for (UnderservedPopulationEnum population : list) {
        dbSet.add(population);
      }
      dbWorkspace.setUnderservedPopulationSet(dbSet);
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

  private org.pmiops.workbench.firecloud.model.Workspace
      attemptFirecloudWorkspaceCreation(FirecloudWorkspaceId workspaceId) {
    try {
      fireCloudService.createWorkspace(workspaceId.getWorkspaceNamespace(),
          workspaceId.getWorkspaceName());
      return fireCloudService.getWorkspace(workspaceId.getWorkspaceNamespace(),
          workspaceId.getWorkspaceName()).getWorkspace();
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      throw ExceptionUtils.convertFirecloudException(e);
    }
  }

  @Override
  public ResponseEntity<Workspace> createWorkspace(Workspace workspace) {
    if (Strings.isNullOrEmpty(workspace.getNamespace())) {
      throw new BadRequestException("missing required field 'namespace'");
    } else if (Strings.isNullOrEmpty(workspace.getName())) {
      throw new BadRequestException("missing required field 'name'");
    } else if (workspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'researchPurpose'");
    } else if (workspace.getDataAccessLevel() == null) {
      throw new BadRequestException("missing required field 'dataAccessLevel'");
    }
    User user = userProvider.get();
    org.pmiops.workbench.db.model.Workspace existingWorkspace = workspaceService.getByName(
        workspace.getNamespace(), workspace.getName());
    if (existingWorkspace != null) {
      throw new ConflictException(String.format(
          "Workspace %s/%s already exists",
          workspace.getNamespace(), workspace.getName()));
    }

    // Note: please keep any initialization logic here in sync with CloneWorkspace().
    FirecloudWorkspaceId workspaceId = generateFirecloudWorkspaceId(workspace.getNamespace(),
        workspace.getName());
    FirecloudWorkspaceId fcWorkspaceId = workspaceId;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace = null;
    for (int attemptValue = 0; attemptValue < MAX_FC_CREATION_ATTEMPT_VALUES; attemptValue++) {
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

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        new org.pmiops.workbench.db.model.Workspace();
    dbWorkspace.setFirecloudName(fcWorkspaceId.getWorkspaceName());
    dbWorkspace.setWorkspaceNamespace(fcWorkspaceId.getWorkspaceNamespace());
    dbWorkspace.setCreator(user);
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);
    setCdrVersionId(dbWorkspace, workspace.getCdrVersionId());

    org.pmiops.workbench.db.model.Workspace reqWorkspace = FROM_CLIENT_WORKSPACE.apply(workspace);
    // TODO: enforce data access level authorization
    dbWorkspace.setDataAccessLevel(reqWorkspace.getDataAccessLevel());
    dbWorkspace.setName(reqWorkspace.getName());
    dbWorkspace.setDescription(reqWorkspace.getDescription());

    // Ignore incoming fields pertaining to review status; clients can only request a review.
    setResearchPurposeDetails(dbWorkspace, workspace.getResearchPurpose());
    if (reqWorkspace.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(reqWorkspace.getReviewRequested());

    org.pmiops.workbench.db.model.WorkspaceUserRole permissions = new org.pmiops.workbench.db.model.WorkspaceUserRole();
    permissions.setRole(WorkspaceAccessLevel.OWNER);
    permissions.setWorkspace(dbWorkspace);
    permissions.setUser(user);

    dbWorkspace.addWorkspaceUserRole(permissions);

    dbWorkspace = workspaceService.getDao().save(dbWorkspace);
    return ResponseEntity.ok(TO_SINGLE_CLIENT_WORKSPACE_FROM_FC_AND_DB.apply(dbWorkspace, fcWorkspace));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteWorkspace(String workspaceNamespace, String workspaceId) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);
    try {
      // This automatically handles access control to the workspace.
      fireCloudService.deleteWorkspace(workspaceNamespace, workspaceId);
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      throw ExceptionUtils.convertFirecloudException(e);
    }
    workspaceService.getDao().delete(dbWorkspace);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<List<FileDetail>> getNoteBookList(String workspaceNamespace,
      String workspaceId) {
    List<FileDetail> fileList = new ArrayList<>();
    try {
      org.pmiops.workbench.firecloud.model.Workspace fireCloudWorkspace =
          fireCloudService.getWorkspace(workspaceNamespace, workspaceId)
              .getWorkspace();
      String bucketName = fireCloudWorkspace.getBucketName();
      fileList = getFilesFromNotebooks(bucketName);
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      if (e.getCode() == 404) {
        throw new NotFoundException(String.format("Workspace %s/%s not found",
            workspaceNamespace, workspaceId));
      }
      throw new ServerErrorException(e);
    }
    return ResponseEntity.ok(fileList);
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(String workspaceNamespace, String workspaceId) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse;
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace;

    WorkspaceResponse response = new WorkspaceResponse();

    try {
      // This enforces access controls.
      fcResponse = fireCloudService.getWorkspace(
          workspaceNamespace, workspaceId);
      fcWorkspace = fcResponse.getWorkspace();
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      throw ExceptionUtils.convertFirecloudException(e);
    }

    if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      // We don't expose PROJECT_OWNER in our API; just use OWNER.
      response.setAccessLevel(WorkspaceAccessLevel.OWNER);
    } else {
      response.setAccessLevel(WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel()));
      if (response.getAccessLevel() == null) {
        throw new ServerErrorException("Unsupported access level: " + fcResponse.getAccessLevel());
      }
    }
    response.setWorkspace(TO_SINGLE_CLIENT_WORKSPACE_FROM_FC_AND_DB.apply(dbWorkspace, fcWorkspace));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    // TODO: use FireCloud to determine what workspaces to return, instead of just returning
    // workspaces from our database.
    User user = userProvider.get();
    List<WorkspaceResponse> responseList = new ArrayList<WorkspaceResponse>();
    if (user != null) {
      for (WorkspaceUserRole userRole : user.getWorkspaceUserRoles()) {
        // TODO: Use FireCloud to determine access roles, not our DB
        WorkspaceResponse currentWorkspace = new WorkspaceResponse();
        currentWorkspace.setWorkspace(TO_CLIENT_WORKSPACE.apply(userRole.getWorkspace()));
        currentWorkspace.setAccessLevel(userRole.getRole());
        responseList.add(currentWorkspace);
      }
    }
    WorkspaceResponseListResponse response = new WorkspaceResponseListResponse();
    response.setItems(responseList);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Workspace> updateWorkspace(String workspaceNamespace, String workspaceId,
      UpdateWorkspaceRequest request) {
    org.pmiops.workbench.db.model.Workspace dbWorkspace = workspaceService.getRequired(
        workspaceNamespace, workspaceId);
    workspaceService.enforceWorkspaceAccessLevel(workspaceNamespace,
        workspaceId, WorkspaceAccessLevel.WRITER);
    Workspace workspace = request.getWorkspace();
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
    if(workspace.getDataAccessLevel() != null &&
        !dbWorkspace.getDataAccessLevel().equals(workspace.getDataAccessLevel())){
      throw new BadRequestException("Attempted to change data access level");
    }
    if (workspace.getDescription() != null) {
      dbWorkspace.setDescription(workspace.getDescription());
    }
    if (workspace.getName() != null) {
      dbWorkspace.setName(workspace.getName());
    }
    // TODO: handle research purpose
    setCdrVersionId(dbWorkspace, workspace.getCdrVersionId());
    // The version asserted on save is the same as the one we read via
    // getRequired() above, see RW-215 for details.
    dbWorkspace = workspaceService.saveWithLastModified(dbWorkspace);
    return ResponseEntity.ok(TO_SINGLE_CLIENT_WORKSPACE.apply(dbWorkspace));
  }

  @Override
  public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(String workspaceNamespace,
      String workspaceId, CloneWorkspaceRequest body) {
    Workspace workspace = body.getWorkspace();
    if (Strings.isNullOrEmpty(workspace.getNamespace())) {
      throw new BadRequestException("missing required field 'workspace.namespace'");
    } else if (Strings.isNullOrEmpty(workspace.getName())) {
      throw new BadRequestException("missing required field 'workspace.name'");
    } else if (workspace.getResearchPurpose() == null) {
      throw new BadRequestException("missing required field 'workspace.researchPurpose'");
    }
    User user = userProvider.get();
    if (workspaceService.getByName(workspace.getNamespace(), workspace.getName()) != null) {
      throw new ConflictException(String.format(
          "Workspace %s/%s already exists",
          workspace.getNamespace(), workspace.getName()));
    }

    // Retrieving the workspace is done first, which acts as an access check.
    String fromBucket = null;
    try {
      fromBucket = fireCloudService.getWorkspace(workspaceNamespace, workspaceId)
          .getWorkspace()
          .getBucketName();
    } catch (ApiException e) {
      if (e.getCode() == 404) {
        log.log(Level.INFO, "Firecloud workspace not found", e);
        throw new NotFoundException(String.format(
            "workspace %s/%s not found or not accessible", workspaceNamespace, workspaceId));
      }
      log.log(Level.SEVERE, "Firecloud server error", e);
      throw new ServerErrorException();
    }
    org.pmiops.workbench.db.model.Workspace fromWorkspace = workspaceService.getRequiredWithCohorts(
        workspaceNamespace, workspaceId);
    if (fromWorkspace == null) {
      throw new NotFoundException(String.format(
          "Workspace %s/%s not found", workspaceNamespace, workspaceId));
    }

    FirecloudWorkspaceId fcWorkspaceId = generateFirecloudWorkspaceId(workspace.getNamespace(),
        workspace.getName());
    fireCloudService.cloneWorkspace(workspaceNamespace, workspaceId,
        fcWorkspaceId.getWorkspaceNamespace(), fcWorkspaceId.getWorkspaceName());

    org.pmiops.workbench.firecloud.model.Workspace toFcWorkspace = null;
    try {
      toFcWorkspace = fireCloudService.getWorkspace(
          fcWorkspaceId.getWorkspaceNamespace(), fcWorkspaceId.getWorkspaceName())
          .getWorkspace();
    } catch (ApiException e) {
      log.log(Level.SEVERE, "Firecloud error retrieving newly cloned workspace", e);
      throw new ServerErrorException();
    }

    // In the future, we may want to allow callers to specify whether notebooks
    // should be cloned at all (by default, yes), else they are currently stuck
    // if someone accidentally adds a large notebook or if there are too many to
    // feasibly copy within a single API request.
    for (Blob b : cloudStorageService.getBlobList(fromBucket, NOTEBOOKS_WORKSPACE_DIRECTORY)) {
      if (!NOTEBOOK_PATTERN.matcher(b.getName()).matches()) {
        continue;
      }
      if (b.getSize() != null && b.getSize()/1e6 > MAX_NOTEBOOK_SIZE_MB) {
        throw new FailedPreconditionException(String.format(
            "workspace %s/%s contains a notebook larger than %dMB: '%s'; cannot clone - please " +
            "remove this notebook, reduce its size, or contact the workspace owner",
            workspaceNamespace, workspaceId, MAX_NOTEBOOK_SIZE_MB, b.getName()));
      }
      cloudStorageService.copyBlob(
          b.getBlobId(), BlobId.of(toFcWorkspace.getBucketName(), b.getName()));
    }

    // The final step in the process is to clone the AoU representation of the
    // workspace. The implication here is that we may generate orphaned
    // Firecloud workspaces / buckets, but a user should not be able to see
    // half-way cloned workspaces via AoU - so it will just appear as a
    // transient failure.
    org.pmiops.workbench.db.model.Workspace toWorkspace =
        FROM_CLIENT_WORKSPACE.apply(body.getWorkspace());
    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        new org.pmiops.workbench.db.model.Workspace();

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbWorkspace.setFirecloudName(fcWorkspaceId.getWorkspaceName());
    dbWorkspace.setWorkspaceNamespace(fcWorkspaceId.getWorkspaceNamespace());
    dbWorkspace.setCreator(user);
    dbWorkspace.setCreationTime(now);
    dbWorkspace.setLastModifiedTime(now);
    dbWorkspace.setVersion(1);

    dbWorkspace.setName(toWorkspace.getName());
    ResearchPurpose researchPurpose = body.getWorkspace().getResearchPurpose();
    setResearchPurposeDetails(dbWorkspace, researchPurpose);
    if (researchPurpose.getReviewRequested()) {
      // Use a consistent timestamp.
      dbWorkspace.setTimeRequested(now);
    }
    dbWorkspace.setReviewRequested(researchPurpose.getReviewRequested());

    // Clone the previous description, by default.
    if (Strings.isNullOrEmpty(toWorkspace.getDescription())) {
      dbWorkspace.setDescription(fromWorkspace.getDescription());
    } else {
      dbWorkspace.setDescription(toWorkspace.getDescription());
    }

    dbWorkspace.setCdrVersion(fromWorkspace.getCdrVersion());
    dbWorkspace.setDataAccessLevel(fromWorkspace.getDataAccessLevel());

    org.pmiops.workbench.db.model.WorkspaceUserRole permissions = new org.pmiops.workbench.db.model.WorkspaceUserRole();
    permissions.setRole(WorkspaceAccessLevel.OWNER);
    permissions.setWorkspace(dbWorkspace);
    permissions.setUser(user);

    dbWorkspace.addWorkspaceUserRole(permissions);

    dbWorkspace = workspaceService.saveAndCloneCohorts(fromWorkspace, dbWorkspace);
    CloneWorkspaceResponse resp = new CloneWorkspaceResponse();
    resp.setWorkspace(TO_SINGLE_CLIENT_WORKSPACE_FROM_FC_AND_DB.apply(dbWorkspace, toFcWorkspace));
    return ResponseEntity.ok(resp);
  }

  @Override
  public ResponseEntity<ShareWorkspaceResponse> shareWorkspace(String workspaceNamespace, String workspaceId,
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
    for (UserRole user : request.getItems()) {
      WorkspaceUserRole newUserRole = new WorkspaceUserRole();
      User newUser = userDao.findUserByEmail(user.getEmail());
      if (newUser == null) {
        throw new BadRequestException(String.format(
            "User %s doesn't exist",
            user.getEmail()));
      }
      newUserRole.setUser(newUser);
      newUserRole.setRole(user.getRole());
      dbUserRoles.add(newUserRole);
    }
    // This automatically enforces owner role.
    dbWorkspace = workspaceService.updateUserRoles(dbWorkspace, dbUserRoles);
    ShareWorkspaceResponse resp = new ShareWorkspaceResponse();
    resp.setWorkspaceEtag(Etags.fromVersion(dbWorkspace.getVersion()));
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
    response.setItems(workspaces.stream().map(TO_CLIENT_WORKSPACE).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  /**
   * Returns List of python fileDetails from notebooks folder
   * @param bucketName
   * @return list of FileDetail
   * @throws org.pmiops.workbench.firecloud.ApiException
   */
  private List<FileDetail> getFilesFromNotebooks(String bucketName) throws org.pmiops.workbench.firecloud.ApiException {
    List<Blob> blobList = new ArrayList<>();
    blobList = cloudStorageService.getBlobList(bucketName, NOTEBOOKS_WORKSPACE_DIRECTORY);
    blobList = blobList.stream()
        .filter(blob ->
            blob.getName().matches("([^\\s]+(\\.(?i)(ipynb))$)"))
        .collect(Collectors.toList());
    return convertBlobToFileDetail(blobList, bucketName);

  }

  /**
   * Convers Blob to FileDetail
   * @param blobList
   * @param bucketName
   * @return List of FileDetail
   */
  private List<FileDetail> convertBlobToFileDetail(List<Blob> blobList, String bucketName) {
    List<FileDetail> fileList = new ArrayList<>();
    blobList.forEach(blob -> {
      String[] parts = blob.getName().split("/");
      FileDetail fileDetail = new FileDetail();
      fileDetail.setName(parts[parts.length-1]);
      fileDetail.setPath("gs://" + bucketName + "/" + blob.getName());
      fileList.add(fileDetail);
    });
    return fileList;
  }
}
