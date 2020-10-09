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

  // Describe whether to upload via Data Manipulation Language (DML, i.e. Insert
  // QueryJobConfiguration) or Streaming upload via InsertAllRequest.
  private enum UploadMethod {
    DML,
    STREAMING;
  }

  private final ReportingSnapshotService reportingSnapshotService;
  private final ReportingUploadService reportingUploadServiceDmlImpl;
  private final ReportingUploadService reportingUploadServiceStreamingImpl;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public ReportingServiceImpl(
      @Qualifier("REPORTING_UPLOAD_SERVICE_DML_IMPL")
          ReportingUploadService reportingUploadServiceDmlImpl,
      @Qualifier("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
          ReportingUploadService reportingUploadServiceStreamingImpl,
      ReportingSnapshotService reportingSnapshotService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.reportingUploadServiceDmlImpl = reportingUploadServiceDmlImpl;
    this.reportingUploadServiceStreamingImpl = reportingUploadServiceStreamingImpl;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingSnapshotService = reportingSnapshotService;
  }

  @Override
  public void takeAndUploadSnapshot() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    getConfiguredUploadService().uploadSnapshot(snapshot);
  }

  /*
   * Currently, we can't access appplication config values at initialization time (since they're
   * all request-scoped). This method is a workaround, and selects the upload service implementation
   * whenever a request comes in. The main cost is an extra configured Bean in the ApplicationContext.
   */
  private ReportingUploadService getConfiguredUploadService() {
    final String uploadMethod = workbenchConfigProvider.get().reporting.uploadMethod;
    if (UploadMethod.DML.name().equals(uploadMethod)) {
      return reportingUploadServiceDmlImpl;
    } else if (UploadMethod.STREAMING.name().equals(uploadMethod)) {
      return reportingUploadServiceStreamingImpl;
    } else {
      throw new IllegalArgumentException(
          String.format("Unrecognized upload method %s", uploadMethod));
    }
  }
}
