package org.pmiops.workbench.vwb.admin;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import jakarta.inject.Provider;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbAdminQueryServiceImpl implements VwbAdminQueryService {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  private static final String VWB_WORKSPACE_TABLE = "wsm_workspaces";
  private static final String VWB_WORKSPACE_ACTIVITY_LOG_TABLE = "wsm_workspace_activity_logs";
  private static final String VWB_WORKSPACE_SAM_USERS_TABLE = "sam_workspace_users";
  private static final String VWB_WORKSPACE_CREATOR_COLUMN = "created_by_email";
  private static final String VWB_WORKSPACE_ID_COLUMN = "workspace_user_facing_id";
  private static final String VWB_WORKSPACE_NAME_COLUMN = "workspace_display_name";

  private static final String QUERY =
      "SELECT \n"
          + "  workspace_id, \n"
          + "  workspace_user_facing_id, \n"
          + "  workspace_display_name,\n"
          + "  description, \n"
          + "  created_by_email, \n"
          + "  created_date, \n"
          + "  gcp_project_id, \n"
          + "  pod_user_facing_id, \n"
          + "FROM \n"
          + "  %s \n"
          + "WHERE \n"
          + " %s=@SEARCH_PARAM ";

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
          + "  w.pod_user_facing_id, \n"
          + "FROM \n"
          + " %s w \n"
          + "JOIN \n"
          + " %s wal ON w.workspace_id = wal.workspace_id \n"
          + "WHERE \n"
          + " wal.change_type='GRANT_WORKSPACE_ROLE' \n"
          + "AND \n"
          + " (wal.change_subject_id=@SEARCH_PARAM OR wal.actor_email=@SEARCH_PARAM) ";

  @Autowired
  public VwbAdminQueryServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByCreator(String email) {

    final String queryString =
        String.format(QUERY, getTableName(VWB_WORKSPACE_TABLE), VWB_WORKSPACE_CREATOR_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(email))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByUserFacingId(String id) {

    final String queryString =
        String.format(QUERY, getTableName(VWB_WORKSPACE_TABLE), VWB_WORKSPACE_ID_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(id))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByName(String name) {

    final String queryString =
        String.format(QUERY, getTableName(VWB_WORKSPACE_TABLE), VWB_WORKSPACE_NAME_COLUMN);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(name))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByShareActivity(String email) {

    final String queryString =
        String.format(
            SHARED_QUERY,
            getTableName(VWB_WORKSPACE_TABLE),
            getTableName(VWB_WORKSPACE_ACTIVITY_LOG_TABLE));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("SEARCH_PARAM", QueryParameterValue.string(email))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  @Override
  public List<UserRole> queryVwbWorkspaceCollaboratorsByUserFacingId(String id) {
    final String queryString =
        String.format(
            COLLABORATOR_QUERY,
            getTableName(VWB_WORKSPACE_SAM_USERS_TABLE),
            getTableName(VWB_WORKSPACE_TABLE));

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("USER_FACING_ID", QueryParameterValue.string(id))
            .build();
    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return StreamSupport.stream(result.iterateAll().spliterator(), false)
        .map(this::fieldValueListToUserRole)
        .collect(Collectors.toList());
  }

  private String getTableName(String table) {
    final VwbConfig vwbConfig = workbenchConfigProvider.get().vwb;
    return String.format(
        "`%s.%s.%s`",
        vwbConfig.adminBigQuery.logProjectId, vwbConfig.adminBigQuery.bigQueryDataset, table);
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
    FieldValues.getString(row, "pod_user_facing_id").ifPresent(vwbWorkspace::setPodUserFacingId);

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
}
