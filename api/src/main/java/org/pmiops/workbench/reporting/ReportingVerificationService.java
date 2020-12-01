package org.pmiops.workbench.reporting;

import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUploadDetails;

public interface ReportingVerificationService {

  ReportingUploadDetails getUploadDetails(ReportingSnapshot reportingSnapshot);

  ReportingUploadDetails verifyAndLog(ReportingSnapshot reportingSnapshot);
}
