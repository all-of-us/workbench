package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;

/**
 * Service to upload a pre-compiled ReportingSnapshot to the appropriate Reporting dataset and
 * tables for this project. Each implementation of this service corresponds to a separate BigQuery
 * insertion path.
 */
public interface ReportingUploadService {
  boolean uploadSnapshot(ReportingSnapshot reportingSnapshot);

  void uploadWorkspaceBatch(List<ReportingWorkspace> batch, long captureTimestamp);

  void uploadUserBatch(List<ReportingUser> batch, long captureTimestamp);

  void uploadCohortBatch(List<ReportingCohort> batch, long captureTimestamp);

  void uploadNewUserSatisfactionSurveyBatch(
      List<ReportingNewUserSatisfactionSurvey> batch, long captureTimestamp);

  /** Uploads a record into VerifiedSnapshot table if upload result is verified. */
  void uploadVerifiedSnapshot(long captureTimestamp);
}
