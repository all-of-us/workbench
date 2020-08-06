package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.reporting.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineReportingController implements OfflineReportingApiDelegate {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ReportingService reportingService;

  public OfflineReportingController(
      Provider<WorkbenchConfig> workbenchConfigProvider, ReportingService reportingService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingService = reportingService;
  }

  @Override
  public ResponseEntity<Void> uploadReportingSnapshot() {
    if (workbenchConfigProvider.get().featureFlags.enableReportingUploadCron) {
      reportingService.takeAndUploadSnapshot();
    }
    return ResponseEntity.noContent().build();
  }
}
