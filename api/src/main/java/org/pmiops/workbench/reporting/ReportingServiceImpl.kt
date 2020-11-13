package org.pmiops.workbench.reporting

import org.pmiops.workbench.config.WorkbenchConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import javax.inject.Provider

/*
* Calls the ReportingSnapshotService to obtain the application data from MySQL, Terra (soon),
* and possibly other sources, then calls the uploadSnapshot() method on the configured ReportingUploadService
* to upload to various tables in the BigQuery dataset. There is virtually no business logic in this class
* except for the method for choosing the correct upload implementation.
*/
@Service
class ReportingServiceImpl(
        @param:Qualifier("REPORTING_UPLOAD_SERVICE_DML_IMPL") private val reportingUploadServiceDmlImpl: ReportingUploadService,
        @param:Qualifier("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL") private val reportingUploadServiceStreamingImpl: ReportingUploadService,
        private val reportingSnapshotService: ReportingSnapshotService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) : ReportingService {
    // Describe whether to upload via Data Manipulation Language (DML, i.e. Insert
    // QueryJobConfiguration) or Streaming upload via InsertAllRequest.
    private enum class UploadMethod {
        DML, STREAMING
    }

    override fun takeAndUploadSnapshot() {
        val snapshot = reportingSnapshotService.takeSnapshot()
        configuredUploadService.uploadSnapshot(snapshot!!)
    }

    /*
   * Currently, we can't access appplication config values at initialization time (since they're
   * all request-scoped). This method is a workaround, and selects the upload service implementation
   * whenever a request comes in. The main cost is an extra configured Bean in the ApplicationContext.
   */
    private val configuredUploadService: ReportingUploadService
        private get() {
            val uploadMethod = workbenchConfigProvider.get().reporting.uploadMethod
            return if (UploadMethod.DML.name == uploadMethod) {
                reportingUploadServiceDmlImpl
            } else if (UploadMethod.STREAMING.name == uploadMethod) {
                reportingUploadServiceStreamingImpl
            } else {
                throw IllegalArgumentException(String.format("Unrecognized upload method %s", uploadMethod))
            }
        }
}
