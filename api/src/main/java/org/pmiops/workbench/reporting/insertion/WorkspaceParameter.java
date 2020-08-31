package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.QPV_TIMESTAMP_FORMATTER;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.ROW_TO_INSERT_TIMESTAMP_FORMATTER;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspace;

public enum WorkspaceParameter implements QueryParameterColumn<ReportingWorkspace> {
  WORKSPACE_ID(
      "workspace_id",
      ReportingWorkspace::getWorkspaceId,
      w -> QueryParameterValue.int64(w.getWorkspaceId())),
  CREATOR_ID(
      "creator_id",
      ReportingWorkspace::getCreatorId,
      w -> QueryParameterValue.int64(w.getCreatorId())),
  NAME("name", ReportingWorkspace::getName, w -> QueryParameterValue.string(w.getName())),
  FAKE_SIZE(
      "fake_size",
      ReportingWorkspace::getFakeSize,
      w -> QueryParameterValue.int64(w.getFakeSize())),
  CREATION_TIME(
      "creation_time",
      w -> ROW_TO_INSERT_TIMESTAMP_FORMATTER.format(w.getCreationTime().toInstant()),
      w ->
          QueryParameterValue.timestamp(
              QPV_TIMESTAMP_FORMATTER.format(w.getCreationTime().toInstant())));

  private final String parameterName;
  private final Function<ReportingWorkspace, Object> rowToInsertValueFunction;
  private final Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction;

  WorkspaceParameter(
      String parameterName,
      Function<ReportingWorkspace, Object> rowToInsertValueFunction,
      Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  };

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspace, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }

  @Override
  public Function<ReportingWorkspace, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
