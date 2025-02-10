package org.pmiops.workbench.reporting;

import jakarta.inject.Provider;
import java.util.List;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingBase;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingLeonardoAppUsage;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetConceptSetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.LeonardoAppUsageColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.NewUserSatisfactionSurveyColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserGeneralDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserPartnerDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceFreeTierUsageColumnValueExtractor;
import org.springframework.stereotype.Service;

@Service
public class ReportingTableService {
  // for most entities, tables share a name between the RWB source DB and the BQ destination DB
  private static final String COHORT_TABLE_NAME = "cohort";
  private static final String DATASET_COHORT_BQ_TABLE_NAME = "dataset_cohort";
  private static final String DATASET_COHORT_RWB_TABLE_NAME = "data_set_cohort";
  private static final String DATASET_CONCEPT_SET_BQ_TABLE_NAME = "dataset_concept_set";
  private static final String DATASET_CONCEPT_SET_RWB_TABLE_NAME = "data_set_concept_set";
  private static final String DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME = "dataset_domain_value";
  private static final String DATASET_DOMAIN_ID_VALUE_RWB_TABLE_NAME = "data_set_values";
  private static final String DATASET_BQ_TABLE_NAME = "dataset";
  private static final String DATASET_RWB_TABLE_NAME = "data_set";
  private static final String INSTITUTION_TABLE_NAME = "institution";
  private static final String LEO_APP_USAGE_TABLE_NAME = "leonardo_app_usage";
  private static final String NEW_USER_SATISFACTION_SURVEY_TABLE_NAME =
      "new_user_satisfaction_survey";
  private static final String USER_GENERAL_DISCOVERY_SOURCE_TABLE_NAME =
      "user_general_discovery_source";
  private static final String USER_PARTNER_DISCOVERY_TABLE_NAME = "user_partner_discovery_source";
  private static final String USER_TABLE_NAME = "user";
  private static final String WORKSPACE_FREE_TIER_USAGE_TABLE_NAME = "workspace_free_tier_usage";
  private static final String WORKSPACE_TABLE_NAME = "workspace";

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ReportingQueryService reportingQueryService;

  public ReportingTableService(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ReportingQueryService reportingQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingQueryService = reportingQueryService;
  }

  public List<ReportingTableParams<? extends ReportingBase>> getAll() {
    return List.of(
        cohort(),
        dataset(),
        datasetCohort(),
        datasetConceptSet(),
        datasetDomainIdValue(),
        institution(),
        leoAppUsage(),
        newUserSatisfactionSurvey(),
        user(),
        userGeneralDiscoverySource(),
        userPartnerDiscoverySource(),
        workspace(),
        workspaceFreeTierUsage());
  }

  public final ReportingTableParams<ReportingCohort> cohort() {
    return new ReportingTableParams<>(
        COHORT_TABLE_NAME,
        CohortColumnValueExtractor::values,
        reportingQueryService::getCohortBatch,
        () -> reportingQueryService.getTableRowCount(COHORT_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingDataset> dataset() {
    return new ReportingTableParams<>(
        DATASET_BQ_TABLE_NAME,
        DatasetColumnValueExtractor::values,
        reportingQueryService::getDatasetBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_RWB_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingDatasetCohort> datasetCohort() {
    return new ReportingTableParams<>(
        DATASET_COHORT_BQ_TABLE_NAME,
        DatasetCohortColumnValueExtractor::values,
        reportingQueryService::getDatasetCohortBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_COHORT_RWB_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingDatasetConceptSet> datasetConceptSet() {
    return new ReportingTableParams<>(
        DATASET_CONCEPT_SET_BQ_TABLE_NAME,
        DatasetConceptSetColumnValueExtractor::values,
        reportingQueryService::getDatasetConceptSetBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_CONCEPT_SET_RWB_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingDatasetDomainIdValue> datasetDomainIdValue() {
    return new ReportingTableParams<>(
        DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME,
        DatasetDomainColumnValueExtractor::values,
        reportingQueryService::getDatasetDomainIdValueBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_DOMAIN_ID_VALUE_RWB_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingInstitution> institution() {
    return new ReportingTableParams<>(
        INSTITUTION_TABLE_NAME,
        InstitutionColumnValueExtractor::values,
        reportingQueryService::getInstitutionBatch,
        () -> reportingQueryService.getTableRowCount(INSTITUTION_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingLeonardoAppUsage> leoAppUsage() {
    return new ReportingTableParams<>(
        LEO_APP_USAGE_TABLE_NAME,
        LeonardoAppUsageColumnValueExtractor::values,
        reportingQueryService::getLeonardoAppUsageBatch,
        () ->
            reportingQueryService.getAppUsageRowCount(
                workbenchConfigProvider.get().reporting.terraWarehouseLeoAppUsageTableId));
  }

  public final ReportingTableParams<ReportingNewUserSatisfactionSurvey>
      newUserSatisfactionSurvey() {
    return new ReportingTableParams<>(
        NEW_USER_SATISFACTION_SURVEY_TABLE_NAME,
        NewUserSatisfactionSurveyColumnValueExtractor::values,
        reportingQueryService::getNewUserSatisfactionSurveyBatch,
        () -> reportingQueryService.getTableRowCount(NEW_USER_SATISFACTION_SURVEY_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingUser> user() {
    return new ReportingTableParams<>(
        USER_TABLE_NAME,
        UserColumnValueExtractor::values,
        reportingQueryService::getUserBatch,
        () -> reportingQueryService.getTableRowCount(USER_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingUserGeneralDiscoverySource>
      userGeneralDiscoverySource() {
    return new ReportingTableParams<>(
        USER_GENERAL_DISCOVERY_SOURCE_TABLE_NAME,
        UserGeneralDiscoverySourceColumnValueExtractor::values,
        reportingQueryService::getUserGeneralDiscoverySourceBatch,
        () -> reportingQueryService.getTableRowCount(USER_GENERAL_DISCOVERY_SOURCE_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingUserPartnerDiscoverySource>
      userPartnerDiscoverySource() {
    return new ReportingTableParams<>(
        USER_PARTNER_DISCOVERY_TABLE_NAME,
        UserPartnerDiscoverySourceColumnValueExtractor::values,
        reportingQueryService::getUserPartnerDiscoverySourceBatch,
        () -> reportingQueryService.getTableRowCount(USER_PARTNER_DISCOVERY_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingWorkspace> workspace() {
    return new ReportingTableParams<>(
        WORKSPACE_TABLE_NAME,
        WorkspaceColumnValueExtractor::values,
        reportingQueryService::getWorkspaceBatch,
        reportingQueryService::getActiveWorkspaceCount);
  }

  public final ReportingTableParams<ReportingWorkspaceFreeTierUsage> workspaceFreeTierUsage() {
    return new ReportingTableParams<>(
        WORKSPACE_FREE_TIER_USAGE_TABLE_NAME,
        WorkspaceFreeTierUsageColumnValueExtractor::values,
        reportingQueryService::getWorkspaceFreeTierUsageBatch,
        () -> reportingQueryService.getTableRowCount(WORKSPACE_FREE_TIER_USAGE_TABLE_NAME));
  }
}
