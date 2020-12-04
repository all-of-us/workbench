package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDatasetCohort;

public enum DatasetCohortColumnValueExtractor
    implements ColumnValueExtractor<ReportingDatasetCohort> {
  COHORT_ID("cohort_id", ReportingDatasetCohort::getCohortId, dc -> int64(dc.getCohortId())),
  DATASET_ID("dataset_id", ReportingDatasetCohort::getDatasetId, dc -> int64(dc.getDatasetId()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private static final String TABLE_NAME = "dataset_cohort";
  private final String parameterName;
  private final Function<ReportingDatasetCohort, Object> objectValueFunction;
  private final Function<ReportingDatasetCohort, QueryParameterValue> parameterValueFunction;

  DatasetCohortColumnValueExtractor(
      String parameterName,
      Function<ReportingDatasetCohort, Object> objectValueFunction,
      Function<ReportingDatasetCohort, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
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
  public Function<ReportingDatasetCohort, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingDatasetCohort, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
