package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDatasetCohort;

public enum DatasetCohortColumnValueExtractor
    implements ColumnValueExtractor<ReportingDatasetCohort> {
  COHORT_ID("cohort_id", ReportingDatasetCohort::getCohortId),
  DATASET_ID("dataset_id", ReportingDatasetCohort::getDatasetId);

  public static final String TABLE_NAME = "dataset_cohort";

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private final String parameterName;
  private final Function<ReportingDatasetCohort, Object> objectValueFunction;

  DatasetCohortColumnValueExtractor(
      String parameterName, Function<ReportingDatasetCohort, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDatasetCohort, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
