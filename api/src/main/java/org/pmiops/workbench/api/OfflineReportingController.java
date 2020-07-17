package org.pmiops.workbench.api;

import org.pmiops.workbench.reporting.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineReportingController implements OfflineReportingApiDelegate {

  private final ReportingService reportingService;

  public OfflineReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @Override
  public ResponseEntity<Void> uploadReportingSnapshot() {
    reportingService.uploadSnapshot();
    return ResponseEntity.noContent().build();
  }
}
