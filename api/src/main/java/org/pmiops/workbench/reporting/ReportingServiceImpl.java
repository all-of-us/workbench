package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.logging.Logger;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.exceptions.ServerErrorException;
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

  private <T extends ReportingBase> void uploadBatchesForTable(
      ReportingTableParams<T> tableParams, long captureTimestamp) {
    reportingQueryService
        .getBatchedStream(tableParams.rwbBatchQueryFn(), tableParams.batchSize())
        .forEach(batch -> reportingUploadService.uploadBatch(tableParams, batch, captureTimestamp));
  }

  // upload data in batches, verify the counts, and mark this snapshot valid.
  @Transactional
  @Override
  public void collectRecordsAndUpload() {
    final long captureTimestamp = clock.millis();

    // First: Upload the data in batches.

    reportingTableService
        .getAll()
        .forEach(tableParams -> uploadBatchesForTable(tableParams, captureTimestamp));

    // Second: Verify the counts.
    boolean batchUploadSuccess = reportingVerificationService.verifyBatchesAndLog(captureTimestamp);

    // Finally: Mark this snapshot valid by inserting one record into verified_snapshot table.
    if (batchUploadSuccess) {
      reportingUploadService.uploadVerifiedSnapshot(captureTimestamp);
    } else {
      logger.severe("Failed to verify batch upload result");
      throw new ServerErrorException("Failed to verify batch upload result");
    }
  }
}
