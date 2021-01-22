package org.pmiops.workbench.reporting;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calls the ReportingSnapshotService to obtain the application data from MySQL, Terra (soon), and
 * possibly other sources, then calls the uploadSnapshot() method on the configured
 * ReportingUploadService to upload to various tables in the BigQuery dataset.
 *
 * <p>For tables that are extremely large, we obtain them on smaller batches. The current tables
 * are: Workspace. TODO(RW-6145): Support more tables(e.g. User) as we need.
 */
@Service
public class ReportingServiceImpl implements ReportingService {

  private final ReportingSnapshotService reportingSnapshotService;
  private final ReportingQueryService reportingQueryService;
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

  /** Loads data from data source (MySql only for now), then uploads them. */
  @Transactional
  @Override
  public void collectRecordsAndUpload() {
    // First: Obtain the snapshot data.
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    final long captureTimestamp = snapshot.getCaptureTimestamp();
    reportingUploadService.uploadSnapshot(snapshot);

    // Second: Obtain data on smaller batches for larger data.
    // TODO(RW-6175): Verily batch count
    reportingQueryService
        .getWorkspacesStream()
        .forEach(b -> reportingUploadService.uploadBatchWorkspace(b, captureTimestamp));
  }
}
