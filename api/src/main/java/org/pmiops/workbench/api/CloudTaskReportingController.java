package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.ResponseEntities.noContentRun;

import org.pmiops.workbench.model.ReportingUploadQueueTaskRequest;
import org.pmiops.workbench.reporting.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskReportingController implements CloudTaskReportingApiDelegate {

  private final ReportingService reportingService;

  public CloudTaskReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @Override
  public ResponseEntity<Void> processReportingUploadQueueTask(
      ReportingUploadQueueTaskRequest body) {
    return noContentRun(
        () ->
            reportingService.collectRecordsAndUpload(
                body.getTables(), body.getSnapshotTimestamp()));
  }
}
