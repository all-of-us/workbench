package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingCohort;

public enum CohortColumnValueExtractor implements ColumnValueExtractor<ReportingCohort> {
  COHORT_ID("cohort_id", ReportingCohort::getCohortId),
  CREATION_TIME("creation_time", c -> toInsertRowString(c.getCreationTime())),
  CREATOR_ID("creator_id", ReportingCohort::getCreatorId),
  CRITERIA("criteria", ReportingCohort::getCriteria),
  DESCRIPTION("description", ReportingCohort::getDescription),
  LAST_MODIFIED_TIME("last_modified_time", c -> toInsertRowString(c.getLastModifiedTime())),
  NAME("name", ReportingCohort::getName),
  WORKSPACE_ID("workspace_id", ReportingCohort::getWorkspaceId);

  public static final String TABLE_NAME = "cohort";

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private final String parameterName;
  private final Function<ReportingCohort, Object> objectValueFunction;

  CohortColumnValueExtractor(
      String parameterName, Function<ReportingCohort, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getBigQueryTableName() {
    return TABLE_NAME;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingCohort, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
