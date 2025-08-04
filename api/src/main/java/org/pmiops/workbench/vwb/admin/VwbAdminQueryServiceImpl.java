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
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbAdminQueryServiceImpl implements VwbAdminQueryService {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  private static final String QUERY =
      "SELECT \n"
          + "  workspace_id, \n"
          + "  workspace_user_facing_id, \n"
          + "  workspace_display_name,\n"
          + "  description, \n"
          + "  created_date, \n"
          + "FROM \n"
          + "  %s \n"
          + "WHERE \n"
          + " created_by_email=@EMAIL ";

  @Autowired
  public VwbAdminQueryServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<VwbWorkspace> queryVwbWorkspacesByCreator(String email) {

    final String queryString = String.format(QUERY, getTableName());

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("EMAIL", QueryParameterValue.string(email))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToVwbWorkspace(result);
  }

  private String getTableName() {
    final VwbConfig vwbConfig = workbenchConfigProvider.get().vwb;
    return String.format(
        "`%s.%s.%s`",
        vwbConfig.adminBigQuery.logProjectId,
        vwbConfig.adminBigQuery.bigQueryDataset,
        vwbConfig.adminBigQuery.workspaceTable);
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

    FieldValues.getDateTime(row, "created_date")
        .map(OffsetDateTime::toString)
        .ifPresent(vwbWorkspace::setCreationTime);
    return vwbWorkspace;
  }
}
