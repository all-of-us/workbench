package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.bool;
import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDataset;

public enum DatasetColumnValueExtractor implements ColumnValueExtractor<ReportingDataset> {
  CREATION_TIME(
      "creation_time",
      d -> toInsertRowString(d.getCreationTime()),
      d -> toTimestampQpv(d.getCreationTime())),
  CREATOR_ID("creator_id", ReportingDataset::getCreatorId, d -> int64(d.getCreatorId())),
  DATASET_ID("dataset_id", ReportingDataset::getDatasetId, d -> int64(d.getDatasetId())),
  DESCRIPTION("description", ReportingDataset::getDescription, d -> string(d.getDescription())),
  INCLUDES_ALL_PARTICIPANTS(
      "includes_all_participants",
      ReportingDataset::getIncludesAllParticipants,
      d -> bool(d.getIncludesAllParticipants())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      d -> toInsertRowString(d.getLastModifiedTime()),
      d -> toTimestampQpv(d.getLastModifiedTime())),
  NAME("name", ReportingDataset::getName, d -> string(d.getName())),
  PRE_PACKAGED_CONCEPT_SET(
      "pre_packaged_concept_set",
      ReportingDataset::getPrePackagedConceptSet,
      d -> int64(d.getPrePackagedConceptSet())),
  WORKSPACE_ID("workspace_id", ReportingDataset::getWorkspaceId, d -> int64(d.getWorkspaceId()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  public static final String TABLE_NAME = "dataset";
  private final String parameterName;
  private final Function<ReportingDataset, Object> objectValueFunction;
  private final Function<ReportingDataset, QueryParameterValue> parameterValueFunction;

  DatasetColumnValueExtractor(
      String parameterName,
      Function<ReportingDataset, Object> objectValueFunction,
      Function<ReportingDataset, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDataset, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingDataset, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
