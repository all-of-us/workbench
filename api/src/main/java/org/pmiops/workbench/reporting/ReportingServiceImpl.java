package org.pmiops.workbench.reporting;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Calls the ReportingSnapshotService to obtain the application data from MySQL, Terra (soon),
 * and possibly other sources, then calls the uploadSnapshot() method on the configured ReportingUploadService
 * to upload to various tables in the BigQuery dataset. There is virtually no business logic in this class
 * except for the method for choosing the correct upload implementation.
 */
@Service
public class ReportingServiceImpl implements ReportingService {

  private final ReportingSnapshotService reportingSnapshotService;
  private ReportingQueryService reportingQueryService;
  private final ReportingUploadService reportingUploadService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public ReportingServiceImpl(
      ReportingQueryService reportingQueryService,
      ReportingUploadService reportingUploadService,
      ReportingSnapshotService reportingSnapshotService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.reportingQueryService = reportingQueryService;
    this.reportingUploadService = reportingUploadService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingSnapshotService = reportingSnapshotService;
  }

  @Transactional
  @Override
  public void takeAndUploadSnapshot() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    final long captureTimestamp = snapshot.getCaptureTimestamp();
    reportingUploadService.uploadSnapshot(snapshot);

    reportingQueryService
        .getWorkspacesStream()
        .forEach(b -> reportingUploadService.uploadBatch(b, captureTimestamp));
  }

  public void takeAndUploadBatches() {
    // for each source table/query

    // get stream of batches

    // for each batch

    // upload in ReportingUploadService
  }
}
