package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingUser;

public enum CohortColumnValueExtractor implements ColumnValueExtractor<ReportingCohort> {
  COHORT_ID("cohort_id", ReportingCohort::getCohortId, c -> int64(c.getCohortId())),
  CREATION_TIME("creation_time", c -> toInsertRowString(c.getCreationTime()), c -> toTimestampQpv(c.getCreationTime())),
  CREATOR_ID("creator_id", ReportingCohort::getCreatorId, c -> int64(c.getCreatorId())),
  CRITERIA("criteria", ReportingCohort::getCriteria, c -> string(c.getCriteria())),
  DESCRIPTION("description", ReportingCohort::getDescription, c -> string(c.getDescription())),
  LAST_MODIFIED_TIME("last_modified_time", c -> toInsertRowString(c.getLastModifiedTime()), c -> toTimestampQpv(c.getLastModifiedTime())),
  NAME("name", ReportingCohort::getName, c -> string(c.getName())),
  TYPE("type", ReportingCohort::getType, c -> string(c.getType())),
  VERSION("version", ReportingCohort::getVersion, c -> int64(c.getVersion())),
  WORKSPACE_ID("workspace_id", ReportingCohort::getWorkspaceId, c -> int64(c.getWorkspaceId()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  public static final String TABLE_NAME = "cohort";
  private final String parameterName;
  private final Function<ReportingCohort, Object> objectValueFunction;
  private final Function<ReportingCohort, QueryParameterValue> parameterValueFunction;

  CohortColumnValueExtractor(
      String parameterName,
      Function<ReportingCohort, Object> objectValueFunction,
      Function<ReportingCohort, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingCohort, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingCohort, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }

}
