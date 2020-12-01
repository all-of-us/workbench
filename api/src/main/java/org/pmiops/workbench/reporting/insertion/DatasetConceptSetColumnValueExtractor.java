package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;

public enum DatasetConceptSetColumnValueExtractor
    implements ColumnValueExtractor<ReportingDatasetConceptSet> {
  CONCEPT_SET_ID(
      "concept_set_id",
      ReportingDatasetConceptSet::getConceptSetId,
      d -> int64(d.getConceptSetId())),
  DATASET_ID("dataset_id", ReportingDatasetConceptSet::getDatasetId, d -> int64(d.getDatasetId()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private static final String TABLE_NAME = "dataset_concept_set";
  private final String parameterName;
  private final Function<ReportingDatasetConceptSet, Object> objectValueFunction;
  private final Function<ReportingDatasetConceptSet, QueryParameterValue> parameterValueFunction;

  DatasetConceptSetColumnValueExtractor(
      String parameterName,
      Function<ReportingDatasetConceptSet, Object> objectValueFunction,
      Function<ReportingDatasetConceptSet, QueryParameterValue> parameterValueFunction) {
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
  public Function<ReportingDatasetConceptSet, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingDatasetConceptSet, QueryParameterValue>
      getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
