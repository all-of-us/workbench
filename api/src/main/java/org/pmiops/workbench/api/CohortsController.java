package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.genomics.WgsCohortExtractionService;
import org.pmiops.workbench.model.CdrQuery;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortAnnotationsRequest;
import org.pmiops.workbench.model.CohortAnnotationsResponse;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DataTableSpecification;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.model.TerraJob;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortsController implements CohortsApiDelegate {

  @VisibleForTesting static final int MAX_PAGE_SIZE = 10000;
  @VisibleForTesting static final int DEFAULT_PAGE_SIZE = 1000;
  private static final Logger log = Logger.getLogger(CohortsController.class.getName());

  private final WorkspaceService workspaceService;
  private final CohortDao cohortDao;
  private final CdrVersionDao cdrVersionDao;
  private final CohortFactory cohortFactory;
  private final CohortMapper cohortMapper;
  private final CohortReviewDao cohortReviewDao;
  private final WgsCohortExtractionService wgsCohortExtractionService;
  private final ConceptSetDao conceptSetDao;
  private final CohortMaterializationService cohortMaterializationService;
  private Provider<DbUser> userProvider;
  private final Clock clock;
  private final CdrVersionService cdrVersionService;
  private final UserRecentResourceService userRecentResourceService;

  @Autowired
  CohortsController(
      WorkspaceService workspaceService,
      CohortDao cohortDao,
      CdrVersionDao cdrVersionDao,
      CohortFactory cohortFactory,
      CohortMapper cohortMapper,
      CohortReviewDao cohortReviewDao,
      WgsCohortExtractionService wgsCohortExtractionService,
      ConceptSetDao conceptSetDao,
      CohortMaterializationService cohortMaterializationService,
      Provider<DbUser> userProvider,
      Clock clock,
      CdrVersionService cdrVersionService,
      UserRecentResourceService userRecentResourceService) {
    this.workspaceService = workspaceService;
    this.cohortDao = cohortDao;
    this.cdrVersionDao = cdrVersionDao;
    this.cohortFactory = cohortFactory;
    this.cohortMapper = cohortMapper;
    this.cohortReviewDao = cohortReviewDao;
    this.wgsCohortExtractionService = wgsCohortExtractionService;
    this.conceptSetDao = conceptSetDao;
    this.cohortMaterializationService = cohortMaterializationService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.cdrVersionService = cdrVersionService;
    this.userRecentResourceService = userRecentResourceService;
  }

  private void checkForDuplicateCohortNameException(String newCohortName, DbWorkspace workspace) {
    if (cohortDao.findCohortByNameAndWorkspaceId(newCohortName, workspace.getWorkspaceId())
        != null) {
      throw new BadRequestException(
          String.format(
              "Cohort \"/%s/%s/%s\" already exists.",
              workspace.getWorkspaceNamespace(), workspace.getWorkspaceId(), newCohortName));
    }
  }

  @Override
  public ResponseEntity<Cohort> createCohort(
      String workspaceNamespace, String workspaceId, Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    DbWorkspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    try {
      // validate the cohort definition
      new Gson().fromJson(cohort.getCriteria(), SearchRequest.class);
    } catch (JsonSyntaxException e) {
      throw new ServerErrorException(
          String.format(
              "Could not save Cohort (\"/%s/%s/%s\")",
              workspace.getWorkspaceNamespace(), workspace.getWorkspaceId(), cohort.getName()),
          e);
    }
    checkForDuplicateCohortNameException(cohort.getName(), workspace);

    DbCohort newCohort =
        cohortFactory.createCohort(cohort, userProvider.get(), workspace.getWorkspaceId());
    try {
      // TODO Make this a pre-check within a transaction?
      newCohort = cohortDao.save(newCohort);
      userRecentResourceService.updateCohortEntry(
          workspace.getWorkspaceId(), userProvider.get().getUserId(), newCohort.getCohortId());
    } catch (DataIntegrityViolationException e) {
      // TODO The exception message doesn't show up anywhere; neither logged nor returned to the
      // client by Spring (the client gets a default reason string).
      throw new ServerErrorException(
          String.format(
              "Could not save Cohort (\"/%s/%s/%s\")",
              workspace.getWorkspaceNamespace(), workspace.getWorkspaceId(), newCohort.getName()),
          e);
    }

    return ResponseEntity.ok(cohortMapper.dbModelToClient(newCohort));
  }

  @Override
  public ResponseEntity<Cohort> duplicateCohort(
      String workspaceNamespace, String workspaceId, DuplicateCohortRequest params) {
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    DbWorkspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    checkForDuplicateCohortNameException(params.getNewName(), workspace);

    DbCohort originalCohort =
        getDbCohort(workspaceNamespace, workspaceId, params.getOriginalCohortId());
    DbCohort newCohort =
        cohortFactory.duplicateCohort(
            params.getNewName(), userProvider.get(), workspace, originalCohort);
    try {
      newCohort = cohortDao.save(newCohort);
      userRecentResourceService.updateCohortEntry(
          workspace.getWorkspaceId(), userProvider.get().getUserId(), newCohort.getCohortId());
    } catch (Exception e) {
      throw new ServerErrorException(
          String.format(
              "Could not save Cohort (\"/%s/%s/%s\")",
              workspace.getWorkspaceNamespace(), workspace.getWorkspaceId(), newCohort.getName()),
          e);
    }

    return ResponseEntity.ok(cohortMapper.dbModelToClient(newCohort));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohort(
      String workspaceNamespace, String workspaceId, Long cohortId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId);
    cohortDao.delete(dbCohort);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Cohort> getCohort(
      String workspaceNamespace, String workspaceId, Long cohortId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId);
    return ResponseEntity.ok(cohortMapper.dbModelToClient(dbCohort));
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    DbWorkspace workspace =
        workspaceService.getRequiredWithCohorts(workspaceNamespace, workspaceId);
    CohortListResponse response = new CohortListResponse();
    Set<DbCohort> cohorts = workspace.getCohorts();
    if (cohorts != null) {
      response.setItems(
          cohorts.stream()
              .map(cohortMapper::dbModelToClient)
              .sorted(Comparator.comparing(c -> c.getName()))
              .collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(
      String workspaceNamespace, String workspaceId, Long cohortId, Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, workspaceId, cohortId);
    if (Strings.isNullOrEmpty(cohort.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(cohort.getEtag());
    if (dbCohort.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated cohort version");
    }
    if (cohort.getType() != null) {
      dbCohort.setType(cohort.getType());
    }
    if (cohort.getName() != null) {
      dbCohort.setName(cohort.getName());
    }
    if (cohort.getDescription() != null) {
      dbCohort.setDescription(cohort.getDescription());
    }
    if (cohort.getCriteria() != null) {
      dbCohort.setCriteria(cohort.getCriteria());
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbCohort.setLastModifiedTime(now);
    try {
      // The version asserted on save is the same as the one we read via
      // getRequired() above, see RW-215 for details.
      dbCohort = cohortDao.save(dbCohort);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort update", e);
      throw new ConflictException("Failed due to concurrent cohort modification");
    }
    return ResponseEntity.ok(cohortMapper.dbModelToClient(dbCohort));
  }

  private Set<Long> getConceptIds(DbWorkspace workspace, TableQuery tableQuery) {
    String conceptSetName = tableQuery.getConceptSetName();
    if (conceptSetName != null) {
      DbConceptSet conceptSet =
          conceptSetDao.findConceptSetByNameAndWorkspaceId(
              conceptSetName, workspace.getWorkspaceId());
      if (conceptSet == null) {
        throw new NotFoundException(
            String.format(
                "Couldn't find concept set with name %s in workspace %s/%s",
                conceptSetName, workspace.getWorkspaceNamespace(), workspace.getWorkspaceId()));
      }
      String tableName = BigQueryTableInfo.getTableName(conceptSet.getDomainEnum());
      if (tableName == null) {
        throw new ServerErrorException(
            "Couldn't find table for domain: " + conceptSet.getDomainEnum());
      }
      if (!tableName.equals(tableQuery.getTableName())) {
        throw new BadRequestException(
            String.format(
                "Can't use concept set for domain %s with table %s",
                conceptSet.getDomainEnum(), tableQuery.getTableName()));
      }
      return conceptSet.getConceptSetConceptIds().stream()
          .map(DbConceptSetConceptId::getConceptId)
          .collect(Collectors.toSet());
    }
    return null;
  }

  @Override
  public ResponseEntity<MaterializeCohortResponse> materializeCohort(
      String workspaceNamespace, String workspaceId, MaterializeCohortRequest request) {
    // This also enforces registered auth domain.
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DbCdrVersion cdrVersion = workspace.getCdrVersion();

    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(
            String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()));
      }
      cdrVersionService.setCdrVersion(cdrVersion);
    }
    String cohortSpec;
    DbCohortReview cohortReview = null;
    if (request.getCohortName() != null) {
      DbCohort cohort =
          cohortDao.findCohortByNameAndWorkspaceId(
              request.getCohortName(), workspace.getWorkspaceId());
      if (cohort == null) {
        throw new NotFoundException(
            String.format(
                "Couldn't find cohort with name %s in workspace %s/%s",
                request.getCohortName(), workspaceNamespace, workspaceId));
      }
      cohortReview =
          cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
              cohort.getCohortId(), cdrVersion.getCdrVersionId());
      cohortSpec = cohort.getCriteria();
    } else if (request.getCohortSpec() != null) {
      cohortSpec = request.getCohortSpec();
      if (request.getStatusFilter() != null) {
        throw new BadRequestException("statusFilter cannot be used with cohortSpec");
      }
    } else {
      throw new BadRequestException("Must specify either cohortName or cohortSpec");
    }
    Set<Long> conceptIds = null;
    if (request.getFieldSet() != null && request.getFieldSet().getTableQuery() != null) {
      conceptIds = getConceptIds(workspace, request.getFieldSet().getTableQuery());
    }

    Integer pageSize = request.getPageSize();
    if (pageSize == null || pageSize == 0) {
      request.setPageSize(DEFAULT_PAGE_SIZE);
    } else if (pageSize < 0) {
      throw new BadRequestException(
          String.format(
              "Invalid page size: %s; must be between 1 and %d", pageSize, MAX_PAGE_SIZE));
    } else if (pageSize > MAX_PAGE_SIZE) {
      request.setPageSize(MAX_PAGE_SIZE);
    }

    MaterializeCohortResponse response =
        cohortMaterializationService.materializeCohort(
            cohortReview, cohortSpec, conceptIds, request);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CdrQuery> getDataTableQuery(
      String workspaceNamespace, String workspaceId, DataTableSpecification request) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DbCdrVersion cdrVersion = workspace.getCdrVersion();

    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(
            String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()));
      }
      cdrVersionService.setCdrVersion(cdrVersion);
    }
    String cohortSpec;
    DbCohortReview cohortReview = null;
    if (request.getCohortName() != null) {
      DbCohort cohort =
          cohortDao.findCohortByNameAndWorkspaceId(
              request.getCohortName(), workspace.getWorkspaceId());
      if (cohort == null) {
        throw new NotFoundException(
            String.format(
                "Couldn't find cohort with name %s in workspace %s/%s",
                request.getCohortName(), workspaceNamespace, workspaceId));
      }
      cohortReview =
          cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
              cohort.getCohortId(), cdrVersion.getCdrVersionId());
      cohortSpec = cohort.getCriteria();
    } else if (request.getCohortSpec() != null) {
      cohortSpec = request.getCohortSpec();
      if (request.getStatusFilter() != null) {
        throw new BadRequestException("statusFilter cannot be used with cohortSpec");
      }
    } else {
      throw new BadRequestException("Must specify either cohortName or cohortSpec");
    }
    Set<Long> conceptIds = getConceptIds(workspace, request.getTableQuery());
    CdrQuery query =
        cohortMaterializationService.getCdrQuery(cohortSpec, request, cohortReview, conceptIds);
    return ResponseEntity.ok(query);
  }

  @Override
  public ResponseEntity<CohortAnnotationsResponse> getCohortAnnotations(
      String workspaceNamespace, String workspaceId, CohortAnnotationsRequest request) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DbCdrVersion cdrVersion = workspace.getCdrVersion();
    if (request.getCdrVersionName() != null) {
      cdrVersion = cdrVersionDao.findByName(request.getCdrVersionName());
      if (cdrVersion == null) {
        throw new NotFoundException(
            String.format("Couldn't find CDR version with name %s", request.getCdrVersionName()));
      }
    }
    DbCohort cohort =
        cohortDao.findCohortByNameAndWorkspaceId(
            request.getCohortName(), workspace.getWorkspaceId());
    if (cohort == null) {
      throw new NotFoundException(
          String.format(
              "Couldn't find cohort with name %s in workspace %s/%s",
              request.getCohortName(), workspaceNamespace, workspaceId));
    }
    DbCohortReview cohortReview =
        cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(
            cohort.getCohortId(), cdrVersion.getCdrVersionId());
    if (cohortReview == null) {
      return ResponseEntity.ok(
          new CohortAnnotationsResponse().columns(request.getAnnotationQuery().getColumns()));
    }
    return ResponseEntity.ok(cohortMaterializationService.getAnnotations(cohortReview, request));
  }

  @Override
  public ResponseEntity<TerraJob> extractCohortGenomes(
      String workspaceNamespace, String workspaceId, Long cohortId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    if (workspace.getCdrVersion().getWgsBigqueryDataset() == null) {
      throw new BadRequestException("Workspace CDR does not have access to WGS data");
    }

    try {
      return ResponseEntity.ok(
          wgsCohortExtractionService.submitGenomicsCohortExtractionJob(workspace, cohortId));
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      // Given that there are no input arguments ATM, any API exceptions are due to programming or
      // Firecloud errors
      throw new ServerErrorException(e);
    }
  }

  private DbCohort getDbCohort(String workspaceNamespace, String workspaceId, Long cohortId) {
    DbWorkspace workspace = workspaceService.getRequired(workspaceNamespace, workspaceId);

    DbCohort cohort = cohortDao.findOne(cohortId);
    if (cohort == null || cohort.getWorkspaceId() != workspace.getWorkspaceId()) {
      throw new NotFoundException(
          String.format(
              "No cohort with name %s in workspace %s.", cohortId, workspace.getFirecloudName()));
    }
    return cohort;
  }
}
