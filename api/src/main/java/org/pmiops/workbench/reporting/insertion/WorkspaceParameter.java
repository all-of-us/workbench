package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspace;

public enum WorkspaceParameter implements QueryParameterColumn<ReportingWorkspace> {
  WORKSPACE_ID("workspace_id", w -> QueryParameterValue.int64(w.getWorkspaceId())),
  CREATOR_ID("creator_id", w -> QueryParameterValue.int64(w.getCreatorId())),
  FAKE_SIZE("fake_size", w -> QueryParameterValue.int64(w.getFakeSize())),
  CREATION_TIME(
      "creation_time",
      w -> QueryParameterValue.timestamp(w.getCreationTime() * MICROSECODNS_IN_MILLISECOND));

  private final String parameterName;
  private final Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction;

  WorkspaceParameter(
      String parameterName,
      Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.parameterValueFunction = parameterValueFunction;
  };

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspace, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
