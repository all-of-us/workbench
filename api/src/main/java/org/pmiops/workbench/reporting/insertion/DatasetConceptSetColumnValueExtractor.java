package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;

public enum DatasetConceptSetColumnValueExtractor
    implements ColumnValueExtractor<ReportingDatasetConceptSet> {
  CONCEPT_SET_ID("concept_set_id", ReportingDatasetConceptSet::getConceptSetId),
  DATASET_ID("dataset_id", ReportingDatasetConceptSet::getDatasetId);

  public static final String TABLE_NAME = "dataset_concept_set";

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private final String parameterName;
  private final Function<ReportingDatasetConceptSet, Object> objectValueFunction;

  DatasetConceptSetColumnValueExtractor(
      String parameterName, Function<ReportingDatasetConceptSet, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDatasetConceptSet, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
