package org.pmiops.workbench.vwb.admin;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import jakarta.inject.Provider;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig;
import org.pmiops.workbench.model.PreprodWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.VwbDataCollectionEntry;
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.model.VwbWorkspaceAuditLog;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbAdminQueryServiceImpl implements VwbAdminQueryService {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  private static final String VWB_WORKSPACE_TABLE_PREFIX = "wsm_workspaces";
  private static final String VWB_WORKSPACE_ACTIVITY_LOG_TABLE_PREFIX =
      "wsm_workspace_activity_logs";
  private static final String VWB_WORKSPACE_SAM_USERS_TABLE_PREFIX = "sam_workspace_users";
  private static final String VWB_PODS_TABLE_PREFIX = "um_pods";
  private static final String VWB_USER_POD_ROLES_TABLE_PREFIX = "um_user_pod_roles";
  private static final String VWB_RESOURCES_TABLE_PREFIX = "wsm_resources";
  private static final String VWB_WORKSPACE_CREATOR_COLUMN = "created_by_email";
  private static final String VWB_WORKSPACE_ID_COLUMN = "workspace_user_facing_id";
  private static final String VWB_WORKSPACE_GCP_PROJECT_ID_COLUMN = "gcp_project_id";
  private static final String VWB_WORKSPACE_UUID_COLUMN = "workspace_id";
  private static final String VWB_WORKSPACE_NAME_COLUMN = "workspace_display_name";
  private static final String PREPROD_PROJECT_ID = "all-of-us-workbench-test";
  // private static final String PREPROD_PROJECT_ID = "all-of-us-rw-preprod";
  private static final String PREPROD_REPORTING_DATASET = "reporting_test";
  //  private static final String PREPROD_REPORTING_DATASET = "reporting_preprod";
  private static final String PREPROD_REPORTING_WORKSPACES_TABLE = "live_workspace";

  private static final String QUERY =
      "SELECT \n"
          + "  w.workspace_id, \n"
          + "  w.workspace_user_facing_id, \n"
          + "  w.workspace_display_name,\n"
          + "  w.description, \n"
          + "  w.created_by_email, \n"
          + "  w.created_date, \n"
          + "  w.gcp_project_id, \n"
          + "  w.workspace_metadata_policy, \n"
          + "  p.billing_account_id, \n"
          + "FROM \n"
          + "  %s w \n"
          + "JOIN \n"
          + " %s p ON w.pod_id = p.pod_id \n"
          + "WHERE \n"
          + " w.%s=@SEARCH_PARAM ";

  private static final String COLLABORATOR_QUERY =
      "SELECT \n"
          + " swu.role, \n"
          + " swu.user_email, \n"
          + "FROM \n"
          + " %s swu \n"
          + "JOIN \n"
          + " %s w ON swu.workspace_id = w.workspace_id \n"
          + "WHERE \n"
          + " w.workspace_user_facing_id=@USER_FACING_ID ";

  private static final String SHARED_QUERY =
      "SELECT \n"
          + "  w.workspace_id, \n"
          + "  w.workspace_user_facing_id, \n"
          + "  w.workspace_display_name,\n"
          + "  w.description, \n"
          + "  w.created_by_email, \n"
          + "  w.created_date, \n"
          + "  w.gcp_project_id, \n"
          + "  w.workspace_metadata_policy, \n"
          + "  p.billing_account_id, \n"
          + "FROM \n"
          + " %s w \n"
          + "JOIN \n"
          + " %s p ON w.pod_id = p.pod_id \n"
          + "JOIN \n"
          + " %s wal ON w.workspace_id = wal.workspace_id \n"
          + "WHERE \n"
          + " wal.change_type='GRANT_WORKSPACE_ROLE' \n"
          + "AND \n"
          + " (wal.change_subject_id=@SEARCH_PARAM OR wal.actor_email=@SEARCH_PARAM) ";

  private static final String AUDIT_QUERY =
      "SELECT \n"
          + " change_type, \n"
          + " change_date, \n"
          + " actor_email, \n"
          + " workspace_id \n"
          + "FROM \n"
          + " %s \n"
          + "WHERE \n"
          + " change_subject_id=@WORKSPACE_ID "
          + "ORDER BY change_date ASC ";

  private static final String USER_POD_IDS_QUERY =
      "SELECT u.pod_id \n"
          + "FROM %s u \n"
          + "JOIN %s p ON u.pod_id = p.pod_id \n"
          + "WHERE u.user_email=@USER_EMAIL \n"
          + "AND p.billing_account_id IS NOT NULL \n"
          + "AND p.billing_account_id != '' ";

  private static final String DATA_COLLECTION_QUERY =
      "SELECT \n"
          + "  w.workspace_display_name, \n"
          + "  w.workspace_user_facing_id, \n"
          + "  w.description, \n"
          + "  w.created_date, \n"
          + "  w.gcp_project_id, \n"
          + "  w.pod_id, \n"
          + "  r.resource_display_name, \n"
          + "  r.resource_name, \n"
          + "  r.resource_id, \n"
          + "  r.folder_display_name AS version, \n"
          + "  r.resource_type \n"
          + "FROM \n"
          + "  %s w \n"
          + "LEFT JOIN \n"
          + "  %s r ON w.workspace_id = r.workspace_id \n"
          + "WHERE \n"
          + "  w.is_data_collection = true ";

  private static final String PREPROD_QUERY =
      "SELECT \n"
          + "  workspace_id, \n"
          + "  workspace_namespace, \n"
          + "  name,\n"
          + "  access_tier_short_name, \n"
          + "  cdr_version_id, \n"
          + "  rp_ancestry, \n"
          + "  rp_commercial_purpose, \n"
          + "  rp_control_set, \n"
          + "  rp_disease_focused_research, \n"
          + "  rp_disease_of_focus, \n"
          + "  rp_drug_development, \n"
          + "  rp_educational, \n"
          + "  rp_ethics, \n"
          + "  rp_intended_study, \n"
          + "  rp_methods_development, \n"
          + "  rp_other_population_details, \n"
          + "  rp_other_purpose, \n"
          + "  rp_other_purpose_details, \n"
          + "  rp_population_health, \n"
          + "  rp_review_requested, \n"
          + "  rp_scientific_approach, \n"
          + "  rp_social_behavioral, \n"
          + "FROM \n"
          + "  %s w \n"
          + "WHERE \n"
          + " workspace_namespace=@WORKSPACE_NAMESPACE ";

  @Autowired
  public VwbAdminQueryServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByCreator(String email) {
    final String normalizedEmail = normalizeEmail(email);

    final String queryString =
        String.format(
            QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            VWB_WORKSPACE_CREATOR_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(normalizedEmail))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByUserFacingId(String id) {

    final String queryString =
        String.format(
            QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            VWB_WORKSPACE_ID_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(id))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByWorkspaceId(String workspaceId) {

    final String queryString =
        String.format(
            QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            VWB_WORKSPACE_UUID_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(workspaceId))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByName(String name) {

    final String queryString =
        String.format(
            QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            VWB_WORKSPACE_NAME_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(name))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByShareActivity(String email) {
    final String normalizedEmail = normalizeEmail(email);

    final String queryString =
        String.format(
            SHARED_QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            getTableName(VWB_WORKSPACE_ACTIVITY_LOG_TABLE_PREFIX));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(normalizedEmail))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspaceByGcpProjectId(String projectId) {

    final String queryString =
        String.format(
            QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX),
            VWB_WORKSPACE_GCP_PROJECT_ID_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(projectId))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<UserRole> queryVwbWorkspaceCollaboratorsByUserFacingId(String id) {
    final String queryString =
        String.format(
            COLLABORATOR_QUERY,
            getTableName(VWB_WORKSPACE_SAM_USERS_TABLE_PREFIX),
            getTableName(VWB_WORKSPACE_TABLE_PREFIX));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("USER_FACING_ID", QueryParameterValue.string(id))
            .build();
    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(this::fieldValueListToUserRole)
        .collect(Collectors.toList());
  }

  @Override
  public List<VwbWorkspaceAuditLog> queryVwbWorkspaceActivity(String workspaceId) {

    final String queryString =
        String.format(AUDIT_QUERY, getTableName(VWB_WORKSPACE_ACTIVITY_LOG_TABLE_PREFIX));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("WORKSPACE_ID", QueryParameterValue.string(workspaceId))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(this::fieldValueListToVwbWorkspaceAuditLog)
        .collect(Collectors.toList());
  }

  @Override
  public Set<String> queryPodIdsByUserEmail(String email) {
    final String queryString =
        String.format(
            USER_POD_IDS_QUERY,
            getTableName(VWB_USER_POD_ROLES_TABLE_PREFIX),
            getTableName(VWB_PODS_TABLE_PREFIX));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("USER_EMAIL", QueryParameterValue.string(email))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(row -> FieldValues.getString(row, "pod_id"))
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .collect(Collectors.toSet());
  }

  @Override
  public List<VwbDataCollectionEntry> queryVwbDataCollections() {
    final String queryString =
        String.format(
            DATA_COLLECTION_QUERY,
            getTableName(VWB_WORKSPACE_TABLE_PREFIX),
            getTableName(VWB_RESOURCES_TABLE_PREFIX));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString).build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(this::fieldValueListToVwbDataCollectionEntry)
        .collect(Collectors.toList());
  }

  @Override
  public List<PreprodWorkspace> queryPreprodWorkspaceByNamespace(String workspaceNamespace) {

    final String queryString = String.format(PREPROD_QUERY, getPreprodReportingTable());

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter(
                "WORKSPACE_NAMESPACE", QueryParameterValue.string(workspaceNamespace))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(this::fieldValueListToPreprodWorkspace)
        .toList();
  }

  private String getTableName(String tablePrefix) {
    final VwbConfig vwbConfig = workbenchConfigProvider.get().vwb;
    final String fullTableName = String.format("%s_%s", tablePrefix, vwbConfig.organizationUfid);
    return String.format(
        "`%s.%s.%s`",
        vwbConfig.adminBigQuery.logProjectId,
        vwbConfig.adminBigQuery.bigQueryDataset,
        fullTableName);
  }

  private String getPreprodReportingTable() {
    return String.format(
        "`%s.%s.%s`",
        PREPROD_PROJECT_ID, PREPROD_REPORTING_DATASET, PREPROD_REPORTING_WORKSPACES_TABLE);
  }

  private List<VwbWorkspace> tableResultToVwbWorkspace(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToVwbWorkspace)
        .collect(Collectors.toList());
  }

  private VwbWorkspace fieldValueListToVwbWorkspace(FieldValueList row) {
    VwbWorkspace vwbWorkspace = new VwbWorkspace();
    FieldValues.getString(row, "workspace_id").ifPresent(vwbWorkspace::setId);
    FieldValues.getString(row, "workspace_user_facing_id").ifPresent(vwbWorkspace::setUserFacingId);
    FieldValues.getString(row, "workspace_display_name").ifPresent(vwbWorkspace::setDisplayName);
    FieldValues.getString(row, "description").ifPresent(vwbWorkspace::setDescription);
    FieldValues.getString(row, "created_by_email").ifPresent(vwbWorkspace::setCreatedBy);
    FieldValues.getString(row, "gcp_project_id").ifPresent(vwbWorkspace::setGoogleProjectId);
    FieldValues.getString(row, "workspace_metadata_policy")
        .ifPresent(vwbWorkspace::setResearchPurpose);
    FieldValues.getString(row, "billing_account_id").ifPresent(vwbWorkspace::billingAccountId);

    FieldValues.getDateTime(row, "created_date")
        .map(OffsetDateTime::toString)
        .ifPresent(vwbWorkspace::setCreationTime);
    return vwbWorkspace;
  }

  private UserRole fieldValueListToUserRole(FieldValueList row) {
    UserRole userRole = new UserRole();
    FieldValues.getString(row, "role")
        .map((role) -> WorkspaceAccessLevel.valueOf(role.toUpperCase()))
        .ifPresent(userRole::setRole);
    FieldValues.getString(row, "user_email").ifPresent(userRole::setEmail);
    return userRole;
  }

  private VwbWorkspaceAuditLog fieldValueListToVwbWorkspaceAuditLog(FieldValueList row) {
    VwbWorkspaceAuditLog auditLog = new VwbWorkspaceAuditLog();
    FieldValues.getString(row, "workspace_id").ifPresent(auditLog::setWorkspaceId);
    FieldValues.getString(row, "change_type").ifPresent(auditLog::setChangeType);
    FieldValues.getDateTime(row, "change_date")
        .map(OffsetDateTime::toString)
        .ifPresent(auditLog::setChangeTime);
    FieldValues.getString(row, "actor_email").ifPresent(auditLog::setActorEmail);
    return auditLog;
  }

  private VwbDataCollectionEntry fieldValueListToVwbDataCollectionEntry(FieldValueList row) {
    VwbDataCollectionEntry entry = new VwbDataCollectionEntry();
    FieldValues.getString(row, "workspace_display_name").ifPresent(entry::setWorkspaceDisplayName);
    FieldValues.getString(row, "workspace_user_facing_id")
        .ifPresent(entry::setWorkspaceUserFacingId);
    FieldValues.getString(row, "description").ifPresent(entry::setWorkspaceDescription);
    FieldValues.getDateTime(row, "created_date")
        .map(OffsetDateTime::toString)
        .ifPresent(entry::setCreatedDate);
    FieldValues.getString(row, "gcp_project_id").ifPresent(entry::setGcpProjectId);
    FieldValues.getString(row, "pod_id").ifPresent(entry::setPodId);
    FieldValues.getString(row, "resource_display_name").ifPresent(entry::setResourceDisplayName);
    FieldValues.getString(row, "resource_name").ifPresent(entry::setResourceName);
    FieldValues.getString(row, "resource_id").ifPresent(entry::setResourceId);
    FieldValues.getString(row, "version").ifPresent(entry::setVersion);
    FieldValues.getString(row, "resource_type").ifPresent(entry::setResourceType);
    return entry;
  }

  private PreprodWorkspace fieldValueListToPreprodWorkspace(FieldValueList row) {
    PreprodWorkspace preprodWorkspace = new PreprodWorkspace();
    FieldValues.getLong(row, "workspace_id")
        .ifPresent(preprodWorkspace::setWorkspaceId);
    FieldValues.getString(row, "workspace_namespace")
        .ifPresent(preprodWorkspace::setWorkspaceNamespace);
    FieldValues.getString(row, "name").ifPresent(preprodWorkspace::setDisplayName);
    FieldValues.getString(row, "access_tier_short_name")
        .ifPresent(preprodWorkspace::setAccessTierShortName);
    FieldValues.getLong(row, "cdr_version_id")
        .ifPresent(preprodWorkspace::setCdrVersionId);

    ResearchPurpose researchPurpose = new ResearchPurpose();
    FieldValues.getBoolean(row, "rp_ancestry").ifPresent(researchPurpose::setAncestry);
    FieldValues.getBoolean(row, "rp_commercial_purpose")
        .ifPresent(researchPurpose::setCommercialPurpose);
    FieldValues.getBoolean(row, "rp_control_set").ifPresent(researchPurpose::setControlSet);
    FieldValues.getBoolean(row, "rp_disease_focused_research")
        .ifPresent(researchPurpose::setDiseaseFocusedResearch);
    FieldValues.getString(row, "rp_disease_of_focus").ifPresent(researchPurpose::setDiseaseOfFocus);
    FieldValues.getBoolean(row, "rp_drug_development")
        .ifPresent(researchPurpose::setDrugDevelopment);
    FieldValues.getBoolean(row, "rp_educational").ifPresent(researchPurpose::setEducational);
    FieldValues.getBoolean(row, "rp_ethics").ifPresent(researchPurpose::setEthics);
    FieldValues.getString(row, "rp_intended_study").ifPresent(researchPurpose::setIntendedStudy);
    FieldValues.getBoolean(row, "rp_methods_development")
        .ifPresent(researchPurpose::setMethodsDevelopment);
    FieldValues.getString(row, "rp_other_population_details")
        .ifPresent(researchPurpose::setOtherPopulationDetails);
    FieldValues.getBoolean(row, "rp_other_purpose").ifPresent(researchPurpose::setOtherPurpose);
    FieldValues.getString(row, "rp_other_purpose_details")
        .ifPresent(researchPurpose::setOtherPurposeDetails);
    FieldValues.getBoolean(row, "rp_population_health")
        .ifPresent(researchPurpose::setPopulationHealth);
    FieldValues.getBoolean(row, "rp_review_requested")
        .ifPresent(researchPurpose::setReviewRequested);
    FieldValues.getString(row, "rp_scientific_approach")
        .ifPresent(researchPurpose::setScientificApproach);
    FieldValues.getBoolean(row, "rp_social_behavioral")
        .ifPresent(researchPurpose::setSocialBehavioral);
    preprodWorkspace.setResearchPurpose(researchPurpose);
    return preprodWorkspace;
  }

  /**
   * Normalizes email input by appending the configured GSuite domain if no domain is present.
   *
   * @param email The email or username to normalize
   * @return The normalized email address
   */
  private String normalizeEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      return email;
    }

    String trimmedEmail = email.trim();

    // If email already contains '@', return as is (already has a domain)
    if (trimmedEmail.contains("@")) {
      return trimmedEmail;
    }

    // Otherwise, append the configured GSuite domain
    String gSuiteDomain = workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain;
    return trimmedEmail + "@" + gSuiteDomain;
  }
}
