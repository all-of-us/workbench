package org.pmiops.workbench.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetConceptSetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.NewUserSatisfactionSurveyColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserGeneralDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserPartnerDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calls the ReportingQueryService to obtain the application data from MySQL, Terra (soon), and
 * possibly other sources, then calls the upload*Batch() method on the configured
 * ReportingUploadService to upload to various tables in the BigQuery dataset.
 *
 * <p>For large tables (the majority; TODO: all?) we obtain them in batches.
 */
@Service
public class ReportingServiceImpl implements ReportingService {
  private static final Logger logger = Logger.getLogger(ReportingServiceImpl.class.getName());

  private final Clock clock;
  private final ReportingQueryService reportingQueryService;
  private final ReportingUploadService reportingUploadService;
  private final ReportingVerificationService reportingVerificationService;

  @VisibleForTesting
  static final Set<String> BATCH_UPLOADED_TABLES =
      ImmutableSet.of(
          CohortColumnValueExtractor.TABLE_NAME,
          DatasetColumnValueExtractor.TABLE_NAME,
          DatasetCohortColumnValueExtractor.TABLE_NAME,
          DatasetConceptSetColumnValueExtractor.TABLE_NAME,
          DatasetDomainColumnValueExtractor.TABLE_NAME,
          WorkspaceColumnValueExtractor.TABLE_NAME,
          UserColumnValueExtractor.TABLE_NAME,
          NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME,
          UserGeneralDiscoverySourceColumnValueExtractor.TABLE_NAME,
          UserPartnerDiscoverySourceColumnValueExtractor.TABLE_NAME);

  public ReportingServiceImpl(
      Clock clock,
      ReportingQueryService reportingQueryService,
      ReportingUploadService reportingUploadService,
      ReportingVerificationService reportingVerificationService) {
    this.clock = clock;
    this.reportingQueryService = reportingQueryService;
    this.reportingUploadService = reportingUploadService;
    this.reportingVerificationService = reportingVerificationService;
  }

  // upload data in batches, verify the counts, and mark this snapshot valid.
  @Transactional
  @Override
  public void collectRecordsAndUpload() {
    final long captureTimestamp = clock.millis();

    // First: Upload the data in batches.

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
        .getBatchedDatasetStream()
        .forEach(b -> reportingUploadService.uploadDatasetBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedDatasetCohortStream()
        .forEach(b -> reportingUploadService.uploadDatasetCohortBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedDatasetConceptSetStream()
        .forEach(b -> reportingUploadService.uploadDatasetConceptSetBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedDatasetDomainIdValueStream()
        .forEach(b -> reportingUploadService.uploadDatasetDomainIdValueBatch(b, captureTimestamp));
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
    reportingQueryService
        .getBatchedWorkspaceFreeTierUsageStream()
        .forEach(
            b -> reportingUploadService.uploadWorkspaceFreeTierUsageBatch(b, captureTimestamp));
    reportingQueryService
        .getBatchedInstitutionStream()
        .forEach(b -> reportingUploadService.uploadInstitutionBatch(b, captureTimestamp));

    // Second: Verify the counts.
    boolean batchUploadSuccess =
        reportingVerificationService.verifyBatchesAndLog(BATCH_UPLOADED_TABLES, captureTimestamp);

    // Finally: Mark this snapshot valid by inserting one record into verified_snapshot table.
    if (batchUploadSuccess) {
      reportingUploadService.uploadVerifiedSnapshot(captureTimestamp);
    } else {
      logger.warning("Failed to verify batch upload result");
    }
  }
}
