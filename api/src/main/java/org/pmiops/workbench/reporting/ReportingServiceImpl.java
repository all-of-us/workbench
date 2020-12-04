package org.pmiops.workbench.reporting;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/*
 * Calls the ReportingSnapshotService to obtain the application data from MySQL, Terra (soon),
 * and possibly other sources, then calls the uploadSnapshot() method on the configured ReportingUploadService
 * to upload to various tables in the BigQuery dataset. There is virtually no business logic in this class
 * except for the method for choosing the correct upload implementation.
 */
@Service
public class ReportingServiceImpl implements ReportingService {

  private final ReportingSnapshotService reportingSnapshotService;
  private final ReportingUploadService reportingUploadService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public ReportingServiceImpl(
      @Qualifier("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
          ReportingUploadService reportingUploadService,
      ReportingSnapshotService reportingSnapshotService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.reportingUploadService = reportingUploadService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingSnapshotService = reportingSnapshotService;
  }

  @Override
  public void takeAndUploadSnapshot() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    reportingUploadService.uploadSnapshot(snapshot);
  }
}
