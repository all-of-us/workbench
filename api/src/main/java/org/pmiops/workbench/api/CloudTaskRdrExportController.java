package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.rdr.RdrExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskRdrExportController implements CloudTaskRdrExportApiDelegate {

  private RdrExportService rdrExportService;

  private static final Logger log = Logger.getLogger(CloudTaskRdrExportController.class.getName());

  CloudTaskRdrExportController(RdrExportService rdrExportService) {
    this.rdrExportService = rdrExportService;
  }

  /**
   * This endpoint will be called by the task in cloud task queue. It will contain n (specified in
   * workbench config) or less comma separated User Ids whose information needs to be send to
   * RdrExportService
   *
   * @param researcherIds: Type: ArrayList containing user IDs to be exported
   * @return
   */
  @Override
  public ResponseEntity<Void> exportResearcherDataBatch(
      List<Long> researcherIds, Boolean backfill) {
    if (researcherIds == null || researcherIds.isEmpty()) {
      log.severe(" call to export Researcher Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    log.info(String.format("Exporting %d researchers to RDR...", researcherIds.size()));
    rdrExportService.exportUsers(researcherIds, Boolean.TRUE.equals(backfill));

    return ResponseEntity.noContent().build();
  }

  /**
   * Send all the IDS passed in request body to RDRService
   *
   * @param workspaceIds: Type: ArrayList containing Workspace ids to be exported
   * @return
   */
  @Override
  public ResponseEntity<Void> exportWorkspaceDataBatch(List<Long> workspaceIds, Boolean backfill) {
    if (workspaceIds == null || workspaceIds.isEmpty()) {
      log.severe(" call to export Workspace Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    log.info(String.format("Exporting %d workspaces to RDR...", workspaceIds.size()));
    rdrExportService.exportWorkspaces(workspaceIds, Boolean.TRUE.equals(backfill));
    return ResponseEntity.noContent().build();
  }
}
