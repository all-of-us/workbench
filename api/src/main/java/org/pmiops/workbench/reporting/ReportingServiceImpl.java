package org.pmiops.workbench.reporting;

import java.time.Clock;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.ReportingUploadVerificationDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.ReportingBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
  private final ReportingUploadVerificationDao reportingUploadVerificationDao;
  private final TaskQueueService taskQueueService;

  public ReportingServiceImpl(
      Clock clock,
      ReportingTableService reportingTableService,
      ReportingQueryService reportingQueryService,
      ReportingUploadService reportingUploadService,
      ReportingVerificationService reportingVerificationService,
      ReportingUploadVerificationDao reportingUploadVerificationDao,
      TaskQueueService taskQueueService) {
    this.clock = clock;
    this.reportingTableService = reportingTableService;
    this.reportingQueryService = reportingQueryService;
    this.reportingUploadService = reportingUploadService;
    this.reportingVerificationService = reportingVerificationService;
    this.reportingUploadVerificationDao = reportingUploadVerificationDao;
    this.taskQueueService = taskQueueService;
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
    logger.info("Uploading workbench tables in batches");

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

  @Transactional
  @Override
  public void splitUploadIntoTasksAndQueue() {
    final long captureTimestamp = clock.millis();

    boolean hasLatestSnapshotUploaded = reportingUploadVerificationDao.hasLatestSnapshotUploaded();
    if (!hasLatestSnapshotUploaded) {
      logger.severe("Previous snapshot upload did not complete successfully.");
    }

    reportingTableService
        .getAll()
        .forEach(
            tableParams -> {
              reportingUploadVerificationDao.createVerificationEntry(
                  tableParams.bqTableName(), captureTimestamp);
              taskQueueService.pushReportingUploadTask(tableParams.bqTableName(), captureTimestamp);
            });
  }

  // Upload data in batches for specified tables, verify the counts, and check to see if all tables
  // in snapshot have successfully uploaded. If they have, mark snapshot as valid. If they haven't,
  // do nothing
  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public void collectRecordsAndUpload(List<String> tables, long captureTimestamp) {
    reportingTableService
        .getAll(tables)
        .forEach(tableParams -> uploadBatchesForTable(tableParams, captureTimestamp));
    reportingVerificationService.verifyBatchesAndLog(tables, captureTimestamp);

    boolean batchUploadSuccess = reportingVerificationService.verifySnapshot(captureTimestamp);
    if (batchUploadSuccess) {
      reportingUploadService.uploadVerifiedSnapshot(captureTimestamp);
    } else {
      logger.info(
          "Some tables have not been uploaded successfully yet for snapshot at "
              + captureTimestamp);
    }
  }
}
