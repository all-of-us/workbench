package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingLeonardoAppUsage;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

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

  void uploadDatasetBatch(List<ReportingDataset> batch, long captureTimestamp);

  void uploadDatasetCohortBatch(List<ReportingDatasetCohort> batch, long captureTimestamp);

  void uploadDatasetConceptSetBatch(List<ReportingDatasetConceptSet> batch, long captureTimestamp);

  void uploadDatasetDomainIdValueBatch(
      List<ReportingDatasetDomainIdValue> batch, long captureTimestamp);

  void uploadLeonardoAppUsageBatch(List<ReportingLeonardoAppUsage> batch, long captureTimestamp);

  void uploadNewUserSatisfactionSurveyBatch(
      List<ReportingNewUserSatisfactionSurvey> batch, long captureTimestamp);

  void uploadUserGeneralDiscoverySourceBatch(
      List<ReportingUserGeneralDiscoverySource> batch, long captureTimestamp);

  void uploadUserPartnerDiscoverySourceBatch(
      List<ReportingUserPartnerDiscoverySource> batch, long captureTimestamp);

  void uploadWorkspaceFreeTierUsageBatch(
      List<ReportingWorkspaceFreeTierUsage> batch, long captureTimestamp);

  void uploadInstitutionBatch(List<ReportingInstitution> batch, long captureTimestamp);

  /** Uploads a record into VerifiedSnapshot table if upload result is verified. */
  void uploadVerifiedSnapshot(long captureTimestamp);
}
