package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer.MAX_ROWS_PER_INSERT_ALL_REQUEST;

import jakarta.inject.Provider;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ReportingConfig;
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

  // these entities have different names for their RWB source MySQL DB tables and their
  // destination BigQuery tables

  private static final String DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME = "dataset_domain_value";
  private static final String DATASET_DOMAIN_ID_VALUE_RWB_TABLE_NAME = "data_set_values";
  private static final String DATASET_BQ_TABLE_NAME = "dataset";
  private static final String DATASET_RWB_TABLE_NAME = "data_set";

  // The source for Leonardo app usage is the Terra Data Warehouse, which uses BigQuery.
  // The destination table (also in BQ) has the same name, but in a different dataset.
  private static final String LEO_APP_USAGE_BQ_TABLE_NAME = "leonardo_app_usage";

  // all of these entities share a common name between their RWB source MySQL DB tables and their
  // destination BigQuery tables

  private static final String INSTITUTION_TABLE_NAME = "institution";
  private static final String COHORT_TABLE_NAME = "cohort";
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

  public List<ReportingTableParams<? extends ReportingBase>> getAll(List<String> tableNames) {
    var lowerCaseTables = tableNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
    return getAll().stream()
        .filter(table -> lowerCaseTables.contains(table.bqTableName().toLowerCase()))
        .toList();
  }

  private int batchSize(String bqTableName) {
    ReportingConfig config = workbenchConfigProvider.get().reporting;
    int wantedSize =
        Optional.ofNullable(config.batchSizeOverrides)
            .flatMap(overrides -> Optional.ofNullable(overrides.get(bqTableName)))
            .orElse(config.maxRowsPerInsert);

    // don't exceed the max rows allowed by the BQ API
    return Math.min(MAX_ROWS_PER_INSERT_ALL_REQUEST, wantedSize);
  }

  // by default:
  // * use the same table name for the BQ and RWB tables
  // * use the default batch size
  // * use the default row count query
  public final <T extends ReportingBase> ReportingTableParams<T> defaultParams(
      String matchingTableName,
      InsertAllRequestPayloadTransformer<T> bqInsertionBuilder,
      BiFunction<Long, Long, List<T>> rwbBatchQueryFn) {
    return new ReportingTableParams<>(
        matchingTableName,
        batchSize(matchingTableName),
        bqInsertionBuilder,
        rwbBatchQueryFn,
        () -> reportingQueryService.getTableRowCount(matchingTableName));
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
        batchSize(DATASET_BQ_TABLE_NAME),
        DatasetColumnValueExtractor::values,
        reportingQueryService::getDatasetBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_RWB_TABLE_NAME));
  }

  public final ReportingTableParams<ReportingDatasetDomainIdValue> datasetDomainIdValue() {
    return new ReportingTableParams<>(
        DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME,
        batchSize(DATASET_DOMAIN_ID_VALUE_BQ_TABLE_NAME),
        DatasetDomainColumnValueExtractor::values,
        reportingQueryService::getDatasetDomainIdValueBatch,
        () -> reportingQueryService.getTableRowCount(DATASET_DOMAIN_ID_VALUE_RWB_TABLE_NAME));
  }

  // leo app usage queries the Terra Data Warehouse instead of the RWB

  public final ReportingTableParams<ReportingLeonardoAppUsage> leoAppUsage() {
    return new ReportingTableParams<>(
        LEO_APP_USAGE_BQ_TABLE_NAME,
        batchSize(LEO_APP_USAGE_BQ_TABLE_NAME),
        LeonardoAppUsageColumnValueExtractor::values,
        reportingQueryService::getLeonardoAppUsageBatch,
        reportingQueryService::getAppUsageRowCount);
  }

  // workspace only queries for active workspaces, so a simple row count won't match

  public final ReportingTableParams<ReportingWorkspace> workspace() {
    return new ReportingTableParams<>(
        WORKSPACE_TABLE_NAME,
        batchSize(WORKSPACE_TABLE_NAME),
        WorkspaceColumnValueExtractor::values,
        reportingQueryService::getWorkspaceBatch,
        reportingQueryService::getActiveWorkspaceCount);
  }
}
