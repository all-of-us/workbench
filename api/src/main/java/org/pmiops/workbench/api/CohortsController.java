package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.inject.Provider;
import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortMapper;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortsController implements CohortsApiDelegate {

  private static final Logger log = Logger.getLogger(CohortsController.class.getName());

  private final WorkspaceDao workspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final CohortDao cohortDao;
  private final CohortFactory cohortFactory;
  private final CohortMapper cohortMapper;
  private final Provider<DbUser> userProvider;
  private final Clock clock;
  private final UserRecentResourceService userRecentResourceService;

  @Autowired
  CohortsController(
      WorkspaceDao workspaceDao,
      WorkspaceAuthService workspaceAuthService,
      CohortDao cohortDao,
      CohortFactory cohortFactory,
      CohortMapper cohortMapper,
      Provider<DbUser> userProvider,
      Clock clock,
      UserRecentResourceService userRecentResourceService) {
    this.workspaceDao = workspaceDao;
    this.workspaceAuthService = workspaceAuthService;
    this.cohortDao = cohortDao;
    this.cohortFactory = cohortFactory;
    this.cohortMapper = cohortMapper;
    this.userProvider = userProvider;
    this.clock = clock;
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
      String workspaceNamespace, String terraName, Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.WRITER);
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, terraName);

    try {
      // validate the cohort definition
      new Gson().fromJson(cohort.getCriteria(), CohortDefinition.class);
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
      String workspaceNamespace, String terraName, DuplicateCohortRequest params) {
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.WRITER);
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, terraName);

    checkForDuplicateCohortNameException(params.getNewName(), workspace);

    DbCohort originalCohort =
        getDbCohort(workspaceNamespace, terraName, params.getOriginalCohortId());
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
      String workspaceNamespace, String terraName, Long cohortId) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.WRITER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, terraName, cohortId);
    userRecentResourceService.deleteCohortEntry(
        dbCohort.getWorkspaceId(), userProvider.get().getUserId(), dbCohort.getCohortId());
    cohortDao.delete(dbCohort);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Cohort> getCohort(
      String workspaceNamespace, String terraName, Long cohortId) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.READER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, terraName, cohortId);
    return ResponseEntity.ok(cohortMapper.dbModelToClient(dbCohort));
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(
      String workspaceNamespace, String terraName) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.READER);

    DbWorkspace workspace = workspaceDao.getRequiredWithCohorts(workspaceNamespace, terraName);
    CohortListResponse response = new CohortListResponse();
    Set<DbCohort> cohorts = workspace.getCohorts();
    if (cohorts != null) {
      response.setItems(
          cohorts.stream()
              .map(cohortMapper::dbModelToClient)
              .sorted(Comparator.comparing(Cohort::getName))
              .collect(Collectors.toList()));
    }
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(
      String workspaceNamespace, String terraName, Long cohortId, Cohort cohort) {
    // This also enforces registered auth domain.
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, terraName, WorkspaceAccessLevel.WRITER);

    DbCohort dbCohort = getDbCohort(workspaceNamespace, terraName, cohortId);
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
    dbCohort.setLastModifiedBy(userProvider.get().getUsername());
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

  private DbCohort getDbCohort(String workspaceNamespace, String terraName, Long cohortId) {
    DbWorkspace workspace = workspaceDao.getRequired(workspaceNamespace, terraName);

    DbCohort cohort = cohortDao.findById(cohortId).orElse(null);
    if (cohort == null || cohort.getWorkspaceId() != workspace.getWorkspaceId()) {
      throw new NotFoundException(
          String.format(
              "No cohort with name %s in workspace %s.", cohortId, workspace.getFirecloudName()));
    }
    return cohort;
  }
}
