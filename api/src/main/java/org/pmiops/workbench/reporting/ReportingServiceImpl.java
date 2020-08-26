package org.pmiops.workbench.reporting;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ReportingServiceImpl implements ReportingService {

  // Describe whether to upload via Data Manipulation Language (DML, i.e. Insert
  // QueryJobConfiguration)
  // or Streaming upload via InsertAllRequest.
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
  public ReportingJobResult takeAndUploadSnapshot() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    return getConfiguredUploadService().uploadSnapshot(snapshot);
  }

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
