package org.pmiops.workbench.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.NewUserSatisfactionSurveyColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserGeneralDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserPartnerDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
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
  private static final Logger logger = Logger.getLogger(ReportingServiceImpl.class.getName());

  private final ReportingSnapshotService reportingSnapshotService;
  private final ReportingQueryService reportingQueryService;
  private final ReportingUploadService reportingUploadService;
  private final ReportingVerificationService reportingVerificationService;

  @VisibleForTesting
  static final Set<String> BATCH_UPLOADED_TABLES =
      ImmutableSet.of(
          CohortColumnValueExtractor.TABLE_NAME,
          WorkspaceColumnValueExtractor.TABLE_NAME,
          UserColumnValueExtractor.TABLE_NAME,
          NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME,
          UserGeneralDiscoverySourceColumnValueExtractor.TABLE_NAME,
          UserPartnerDiscoverySourceColumnValueExtractor.TABLE_NAME);

  public ReportingServiceImpl(
      ReportingQueryService reportingQueryService,
      ReportingUploadService reportingUploadService,
      ReportingSnapshotService reportingSnapshotService,
      ReportingVerificationService reportingVerificationService) {
    this.reportingQueryService = reportingQueryService;
    this.reportingUploadService = reportingUploadService;
    this.reportingSnapshotService = reportingSnapshotService;
    this.reportingVerificationService = reportingVerificationService;
  }

  /** Loads data from data source (MySql only for now), then uploads them. */
  @Transactional
  @Override
  public void collectRecordsAndUpload() {
    // First: Obtain the snapshot data.
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    final long captureTimestamp = snapshot.getCaptureTimestamp();
    boolean snapshotUploadSuccess = reportingUploadService.uploadSnapshot(snapshot);

    // Second: Obtain data on smaller batches for larger data.
    reportingQueryService
        .getBatchedWorkspaceStream()
        .forEach(b -> reportingUploadService.uploadWorkspaceBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedUserStream()
        .forEach(b -> reportingUploadService.uploadUserBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedCohortStream()
        .forEach(b -> reportingUploadService.uploadCohortBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedLeonardoAppUsageStream()
        .forEach(b -> reportingUploadService.uploadLeonardoAppUsageBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedNewUserSatisfactionSurveyStream()
        .forEach(
            b -> reportingUploadService.uploadNewUserSatisfactionSurveyBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedUserGeneralDiscoverySourceStream()
        .forEach(
            b -> reportingUploadService.uploadUserGeneralDiscoverySourceBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedUserPartnerDiscoverySourceStream()
        .forEach(
            b -> reportingUploadService.uploadUserPartnerDiscoverySourceBatch(b, captureTimestamp));

    // Third: Verify the count.
    boolean batchUploadSuccess =
        reportingVerificationService.verifyBatchesAndLog(BATCH_UPLOADED_TABLES, captureTimestamp);

    // Finally: Mark this snapshot valid by inserting one record into verified_snapshot table.
    if (snapshotUploadSuccess && batchUploadSuccess) {
      reportingUploadService.uploadVerifiedSnapshot(captureTimestamp);
    } else {
      logger.warning(
          String.format(
              "Failed to verify upload result, snapshotUploadSuccess: %s, batchUploadSuccess :%s",
              snapshotUploadSuccess, batchUploadSuccess));
    }
  }
}
