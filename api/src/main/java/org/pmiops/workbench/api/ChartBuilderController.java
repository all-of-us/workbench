package org.pmiops.workbench.api;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.logging.Logger;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.ChartDataListResponse;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChartBuilderController implements ChartBuilderApiDelegate {

  private static final Logger log = Logger.getLogger(ChartBuilderController.class.getName());
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";

  private final ChartService chartService;
  private final WorkspaceAuthService workspaceAuthService;
  private final CohortDao cohortDao;

  @Autowired
  ChartBuilderController(
      ChartService chartService, WorkspaceAuthService workspaceAuthService, CohortDao cohortDao) {
    this.chartService = chartService;
    this.workspaceAuthService = workspaceAuthService;
    this.cohortDao = cohortDao;
  }

  @Override
  public ResponseEntity<ChartDataListResponse> getChartData(
      String workspaceNamespace, String workspaceId, Long cohortId, String domain) {

    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    System.out.println("cohortId:" + cohortId);
    System.out.println("domain:" + domain);
    System.out.println("domain is null?:" + (domain == null));

    Domain domainEnum = null;
    if (domain.length() > 0) {
      domainEnum = getValidDomain(domain);
    }
    //
    CohortDefinition cohortDefinition = getCohortDefinition(dbWorkspace.getWorkspaceId(), cohortId);
    ChartDataListResponse response = new ChartDataListResponse();
    System.out.println("cohortDefinition:" + cohortDefinition);
    return ResponseEntity.ok(
        response.items(chartService.getChartData(cohortDefinition, domainEnum)));
  }

  private Domain getValidDomain(String domain) {
    return Arrays.stream(Domain.values())
        .filter(domainType -> domainType.toString().equalsIgnoreCase(domain))
        .findFirst()
        .orElseThrow(
            () -> new BadRequestException(String.format(BAD_REQUEST_MESSAGE, "domain", domain)));
  }

  private CohortDefinition getCohortDefinition(Long workspaceId, Long cohortId) {
    DbCohort dbCohort = cohortDao.findCohortByWorkspaceIdAndCohortId(workspaceId, cohortId);
    String definition = dbCohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort definition matching cohortId: %s", cohortId));
    }
    return new Gson().fromJson(definition, CohortDefinition.class);
  }
}
