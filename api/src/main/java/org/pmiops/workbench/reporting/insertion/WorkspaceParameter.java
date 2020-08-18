package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.DowncastObject;
import org.pmiops.workbench.model.ReportingWorkspace;

public enum WorkspaceParameter implements QueryParameterColumn<ReportingWorkspace> {
  WORKSPACE_ID("workspace_id", ReportingWorkspace::getWorkspaceId, DowncastObject.INT64),
  CREATOR_ID("creator_id", ReportingWorkspace::getCreatorId, DowncastObject.INT64),
  NAME("name", ReportingWorkspace::getName, DowncastObject.STRING),
  FAKE_SIZE("fake_size", ReportingWorkspace::getFakeSize, DowncastObject.INT64),
  CREATION_TIME("creation_time", w -> MICROSECODNS_IN_MILLISECOND * w.getCreationTime(), DowncastObject.TIMESTAMP_MICROS);

  private final String parameterName;
  private final Function<ReportingWorkspace, Object> objectValueFunction;
  private final Function<Object, QueryParameterValue> parameterValueFunction;

  WorkspaceParameter(
      String parameterName,
      Function<ReportingWorkspace, Object> objectValueFunction,
      Function<Object, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  };

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspace, Object> getObjectValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<Object, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
