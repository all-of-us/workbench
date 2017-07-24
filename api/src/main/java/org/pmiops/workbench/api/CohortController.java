package org.pmiops.workbench.api;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.time.Clock;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortController implements CohortsApi {

  private static final Function<org.pmiops.workbench.db.model.Cohort, Cohort> TO_CLIENT_COHORT =
      new Function<org.pmiops.workbench.db.model.Cohort, Cohort>() {
        @Override
        public Cohort apply(org.pmiops.workbench.db.model.Cohort cohort) {
          Cohort result = new Cohort();
          result.setLastModifiedTime(cohort.getLastModifiedTime());
          if (cohort.getCreator() != null) {
            result.setCreator(cohort.getCreator().getEmail());
          }
          result.setCriteria(cohort.getCriteria());
          result.setCreationTime(cohort.getCreationTime());
          result.setDescription(cohort.getDescription());
          result.setId(cohort.getExternalId());
          result.setName(cohort.getName());
          result.setType(cohort.getType());
          return result;
        }
      };

  private static final Function<Cohort, org.pmiops.workbench.db.model.Cohort> FROM_CLIENT_COHORT =
      new Function<Cohort, org.pmiops.workbench.db.model.Cohort>() {
        @Override
        public org.pmiops.workbench.db.model.Cohort apply(Cohort cohort) {
          org.pmiops.workbench.db.model.Cohort result = new org.pmiops.workbench.db.model.Cohort();
          result.setCriteria(cohort.getCriteria());
          result.setDescription(cohort.getDescription());
          result.setExternalId(cohort.getId());
          result.setName(cohort.getName());
          result.setType(cohort.getType());
          return result;
        }
      };

  private final WorkspaceDao workspaceDao;
  private final CohortDao cohortDao;
  private final Provider<User> userProvider;
  private final Clock clock;

  @Autowired
  CohortController(WorkspaceDao workspaceDao, CohortDao cohortDao, Provider<User> userProvider,
      Clock clock) {
    this.workspaceDao = workspaceDao;
    this.cohortDao = cohortDao;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<Cohort> createCohort(String workspaceNamespace, String workspaceId,
      Cohort cohort) {
    Workspace workspace = getWorkspace(workspaceNamespace, workspaceId);
    DateTime now = new DateTime(clock.instant(), DateTimeZone.UTC);
    org.pmiops.workbench.db.model.Cohort dbCohort = FROM_CLIENT_COHORT.apply(cohort);
    dbCohort.setCreator(userProvider.get());
    dbCohort.setWorkspace(workspace);
    dbCohort.setCreationTime(now);
    dbCohort.setLastModifiedTime(now);
    dbCohort = cohortDao.save(dbCohort);
    return ResponseEntity.ok(TO_CLIENT_COHORT.apply(dbCohort));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    return null;
  }

  @Override
  public ResponseEntity<Cohort> getCohort(String workspaceNamespace, String workspaceId,
      String cohortId) {
    return null;
  }

  @Override
  public ResponseEntity<CohortListResponse> getCohortsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    Workspace workspace = getWorkspace(workspaceNamespace, workspaceId);
    CohortListResponse response = new CohortListResponse();
    response.setItems(workspace.getCohorts().stream().map(TO_CLIENT_COHORT)
        .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Cohort> updateCohort(String workspaceNamespace, String workspaceId,
      String cohortId, Cohort cohort) {
    return null;
  }

  private Workspace getWorkspace(String workspaceNamespace, String workspaceId) {
    String firecloudName = Workspace.toFirecloudName(workspaceNamespace, workspaceId);
    Workspace workspace = workspaceDao.findByFirecloudName(firecloudName);
    if (workspace == null) {
      throw new NotFoundException("No workspace with name {0}".format(firecloudName));
    }
    return workspace;
  }
}
