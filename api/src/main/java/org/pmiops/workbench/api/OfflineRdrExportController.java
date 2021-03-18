package org.pmiops.workbench.api;

import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rdr.RdrExportService;
import org.pmiops.workbench.rdr.RdrTaskQueue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * This offline process is responsible to daily sync up with RDR by creating/pushing multiple Cloud
 * Task tasks with eligible user Ids and workspace Ids to cloud task queue.
 *
 * <p>None of the actual RDR communication occurs from within this cron job handler, so this cron
 * may succeed even though the actual export tasks to RDR are failing. See
 * CloudTaskRdrExportController for the handlers which do the actual RDR export work.
 *
 * @author nsaxena
 */
@RestController
public class OfflineRdrExportController implements OfflineRdrExportApiDelegate {

  private RdrExportService rdrExportService;
  private RdrTaskQueue rdrTaskQueue;

  OfflineRdrExportController(RdrExportService rdrExportService, RdrTaskQueue rdrTaskQueue) {
    this.rdrTaskQueue = rdrTaskQueue;
    this.rdrExportService = rdrExportService;
  }

  /**
   * Purpose of this method will be to create multiple task of 2 categories, user (researcher) and
   * workspace.
   *
   * @return
   */
  @Override
  public ResponseEntity<Void> exportData() {
    // Its important to send all researcher information first to RDR before sending workspace since
    // workspace object will contain collaborator information (userId)
    try {
      rdrTaskQueue.groupIdsAndPushTask(
          rdrExportService.findAllUserIdsToExport(), RdrTaskQueue.EXPORT_RESEARCHER_PATH);
    } catch (Exception ex) {
      throw new ServerErrorException("Error creating RDR export Cloud Tasks for users", ex);
    }
    try {
      rdrTaskQueue.groupIdsAndPushTask(
          rdrExportService.findAllWorkspacesIdsToExport(), RdrTaskQueue.EXPORT_USER_PATH);
    } catch (Exception ex) {
      throw new ServerErrorException("Error creating RDR export Cloud Tasks for workspaces", ex);
    }
    return ResponseEntity.noContent().build();
  }
}
