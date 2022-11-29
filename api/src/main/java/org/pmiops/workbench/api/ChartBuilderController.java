package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.model.ChartDataListResponse;
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

  @Autowired
  ChartBuilderController(ChartService chartService, WorkspaceAuthService workspaceAuthService) {
    this.chartService = chartService;
    this.workspaceAuthService = workspaceAuthService;
  }

  @Override
  public ResponseEntity<ChartDataListResponse> getChartData(
      String workspaceNamespace, String workspaceId, Long cohortId) {

    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    ChartDataListResponse response = new ChartDataListResponse();
    System.out.println("*******CohortId:"+ cohortId);

    return ResponseEntity.ok(response.items(chartService.getChartData()));
  }
}
