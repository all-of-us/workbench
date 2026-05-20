package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingFolderSync;

public enum FolderSyncColumnValueExtractor implements ColumnValueExtractor<ReportingFolderSync> {
  CREATED_BY_USER_ID("created_by_user_id", ReportingFolderSync::getCreatedByUserId),
  STARTED("started", fs -> toInsertRowString(fs.getStarted())),
  FINISHED("finished", fs -> toInsertRowString(fs.getFinished())),
  TRANSFER_JOB_NAME("transfer_job_name", ReportingFolderSync::getTransferJobName),
  TRANSFER_STATE("transfer_state", ReportingFolderSync::getTransferState),
  SOURCE_WORKSPACE_NAMESPACE(
      "source_workspace_namespace", ReportingFolderSync::getSourceWorkspaceNamespace);

  private final String parameterName;
  private final Function<ReportingFolderSync, Object> rowToInsertValueFunction;

  FolderSyncColumnValueExtractor(
      String parameterName, Function<ReportingFolderSync, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingFolderSync, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
