package org.pmiops.workbench.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.rdr.RdrExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskRdrExportController implements CloudTaskRdrExportApiDelegate {

  private RdrExportService rdrExportService;

  private static final Logger log = Logger.getLogger(CloudTaskRdrExportController.class.getName());
  private final String IDS_STRING_SPLIT = ", ";

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
  public ResponseEntity<Void> exportResearcherData(Object researcherIds) {
    if (researcherIds == null || ((ArrayList) researcherIds).isEmpty()) {
      log.severe(" call to export Researcher Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList =
        (ArrayList<Long>)
            ((ArrayList) researcherIds)
                .stream().map(id -> ((Integer) id).longValue()).collect(Collectors.toList());
    rdrExportService.exportUsers(requestUserIdList);

    return ResponseEntity.noContent().build();
  }

  /**
   * Send all the IDS passed in request body to RDRService
   *
   * @param workspaceIds: Type: ArrayList containing Workspace ids to be exported
   * @return
   */
  @Override
  public ResponseEntity<Void> exportWorkspaceData(Object workspaceIds) {
    if (workspaceIds == null || ((ArrayList) workspaceIds).isEmpty()) {
      log.severe(" call to export Workspace Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList = (ArrayList<Long>) workspaceIds;
    rdrExportService.exportWorkspaces(requestUserIdList);
    return ResponseEntity.noContent().build();
  }
}
