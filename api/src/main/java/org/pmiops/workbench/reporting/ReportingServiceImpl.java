package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calls the ReportingQueryService to obtain the application data from MySQL, Terra (soon), and
 * possibly other sources, then calls the upload*Batch() method on the configured
 * ReportingUploadService to upload to various tables in the BigQuery dataset.
 */
@Service
public class ReportingServiceImpl implements ReportingService {
  private static final Logger logger = Logger.getLogger(ReportingServiceImpl.class.getName());

  private final Clock clock;
  private final ReportingTableService reportingTableService;
  private final ReportingQueryService reportingQueryService;
  private final ReportingUploadService reportingUploadService;
  private final ReportingVerificationService reportingVerificationService;

  public ReportingServiceImpl(
      Clock clock,
      ReportingTableService reportingTableService,
      ReportingQueryService reportingQueryService,
      ReportingUploadService reportingUploadService,
      ReportingVerificationService reportingVerificationService) {
    this.clock = clock;
    this.reportingTableService = reportingTableService;
    this.reportingQueryService = reportingQueryService;
    this.reportingUploadService = reportingUploadService;
    this.reportingVerificationService = reportingVerificationService;
  }

  // upload data in batches, verify the counts, and mark this snapshot valid.
  @SuppressWarnings({"unchecked"})
  @Transactional
  @Override
  public void collectRecordsAndUpload() {
    final long captureTimestamp = clock.millis();

    // First: Upload the data in batches.

    reportingTableService
        .getAll()
        .forEach(
            tableParams ->
                reportingQueryService
                    .getBatchedStream(tableParams.rwbBatchQueryFn())
                    .forEach(
                        b ->
                            // if I don't cast these params, I see the error
                            // Incompatible equality constraint: capture of ? extends ReportingBase
                            // and capture of ? extends ReportingBase
                            reportingUploadService.uploadBatch(
                                (ReportingTableParams<ReportingBase>) tableParams,
                                (List<ReportingBase>) b,
                                captureTimestamp)));

    // Second: Verify the counts.
    boolean batchUploadSuccess = reportingVerificationService.verifyBatchesAndLog(captureTimestamp);

    // Finally: Mark this snapshot valid by inserting one record into verified_snapshot table.
    if (batchUploadSuccess) {
      reportingUploadService.uploadVerifiedSnapshot(captureTimestamp);
    } else {
      logger.warning("Failed to verify batch upload result");
    }
  }
}
