package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer.MAX_ROWS_PER_INSERT_ALL_REQUEST;

import jakarta.inject.Provider;
import java.util.List;
import java.util.function.BiFunction;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingBase;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
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
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer;
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

  private final ReportingQueryService reportingQueryService;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

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

  private long defaultBatchSize() {
    return Math.min(
        MAX_ROWS_PER_INSERT_ALL_REQUEST, workbenchConfigProvider.get().reporting.maxRowsPerInsert);
  }

  // by default, use the same table name for the BQ and RWB tables and
  public final <T extends ReportingBase> ReportingTableParams<T> defaultParams(
      String tableName,
      InsertAllRequestPayloadTransformer<T> bqInsertionBuilder,
      BiFunction<Long, Long, List<T>> rwbBatchQueryFn) {
    return new ReportingTableParams<>(
        tableName,
        defaultBatchSize(),
        bqInsertionBuilder,
        rwbBatchQueryFn,
        () -> reportingQueryService.getTableRowCount(tableName));
  }

  public final ReportingTableParams<ReportingCohort> cohort() {
    return defaultParams(
        COHORT_TABLE_NAME,
        CohortColumnValueExtractor::values,
        reportingQueryService::getCohortBatch);
  }

  public final ReportingTableParams<ReportingInstitution> institution() {
    return defaultParams(
        INSTITUTION_TABLE_NAME,
        InstitutionColumnValueExtractor::values,
        reportingQueryService::getInstitutionBatch);
  }

  public final ReportingTableParams<ReportingNewUserSatisfactionSurvey>
      newUserSatisfactionSurvey() {
    return defaultParams(
        NEW_USER_SATISFACTION_SURVEY_TABLE_NAME,
        NewUserSatisfactionSurveyColumnValueExtractor::values,
        reportingQueryService::getNewUserSatisfactionSurveyBatch);
  }

  public final ReportingTableParams<ReportingUser> user() {
    return defaultParams(
        USER_TABLE_NAME, UserColumnValueExtractor::values, reportingQueryService::getUserBatch);
  }

  public final ReportingTableParams<ReportingUserGeneralDiscoverySource>
      userGeneralDiscoverySource() {
    return defaultParams(
        USER_GENERAL_DISCOVERY_SOURCE_TABLE_NAME,
        UserGeneralDiscoverySourceColumnValueExtractor::values,
        reportingQueryService::getUserGeneralDiscoverySourceBatch);
  }

  public final ReportingTableParams<ReportingUserPartnerDiscoverySource>
      userPartnerDiscoverySource() {
    return defaultParams(
        USER_PARTNER_DISCOVERY_TABLE_NAME,
        UserPartnerDiscoverySourceColumnValueExtractor::values,
        reportingQueryService::getUserPartnerDiscoverySourceBatch);
  }

  public final ReportingTableParams<ReportingWorkspaceFreeTierUsage> workspaceFreeTierUsage() {
    return defaultParams(
        WORKSPACE_FREE_TIER_USAGE_TABLE_NAME,
        WorkspaceFreeTierUsageColumnValueExtractor::values,
        reportingQueryService::getWorkspaceFreeTierUsageBatch);
  }

  // dataset and dataset-domain use different names for the RWB and BQ tables

  public final ReportingTableParams<ReportingDataset> dataset() {
    return new ReportingTableParams<>(
        DATASET_BQ_TABLE_NAME,
        defaultBatchSize(),
        DatasetColumnValueExtractor::values,
        reportingQueryService::getDatasetBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_RWB_TABLE_NAME));
  }

  // dataset-domain is also very large with small rows, so it needs a larger batch size

  public final ReportingTableParams<ReportingDatasetDomainIdValue> datasetDomainIdValue() {
    return new ReportingTableParams<>(
        DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME,
        MAX_ROWS_PER_INSERT_ALL_REQUEST,
        DatasetDomainColumnValueExtractor::values,
        reportingQueryService::getDatasetDomainIdValueBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_DOMAIN_ID_VALUE_RWB_TABLE_NAME));
  }

  // leo app usage queries the Terra Data Warehouse instead of the RWB

  public final ReportingTableParams<ReportingLeonardoAppUsage> leoAppUsage() {
    return new ReportingTableParams<>(
        LEO_APP_USAGE_TABLE_NAME,
        defaultBatchSize(),
        LeonardoAppUsageColumnValueExtractor::values,
        reportingQueryService::getLeonardoAppUsageBatch,
        reportingQueryService::getAppUsageRowCount);
  }

  // workspace only queries for active workspaces, so a simple row count won't match

  public final ReportingTableParams<ReportingWorkspace> workspace() {
    return new ReportingTableParams<>(
        WORKSPACE_TABLE_NAME,
        defaultBatchSize(),
        WorkspaceColumnValueExtractor::values,
        reportingQueryService::getWorkspaceBatch,
        reportingQueryService::getActiveWorkspaceCount);
  }
}
