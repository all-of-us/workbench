package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.QPV_TIMESTAMP_FORMATTER;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.ROW_TO_INSERT_TIMESTAMP_FORMATTER;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.BqDtoWorkspace;

public enum WorkspaceParameter implements QueryParameterColumn<BqDtoWorkspace> {
  WORKSPACE_ID(
      "workspace_id",
      BqDtoWorkspace::getWorkspaceId,
      w -> QueryParameterValue.int64(w.getWorkspaceId())),
  CREATOR_ID(
      "creator_id", BqDtoWorkspace::getCreatorId, w -> QueryParameterValue.int64(w.getCreatorId())),
  NAME("name", BqDtoWorkspace::getName, w -> QueryParameterValue.string(w.getName())),
  CREATION_TIME(
      "creation_time",
      w -> ROW_TO_INSERT_TIMESTAMP_FORMATTER.format(w.getCreationTime()),
      w -> QueryParameterValue.timestamp(QPV_TIMESTAMP_FORMATTER.format(w.getCreationTime())));

  private final String parameterName;
  private final Function<BqDtoWorkspace, Object> rowToInsertValueFunction;
  private final Function<BqDtoWorkspace, QueryParameterValue> parameterValueFunction;

  WorkspaceParameter(
      String parameterName,
      Function<BqDtoWorkspace, Object> rowToInsertValueFunction,
      Function<BqDtoWorkspace, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  };

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<BqDtoWorkspace, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }

  @Override
  public Function<BqDtoWorkspace, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
