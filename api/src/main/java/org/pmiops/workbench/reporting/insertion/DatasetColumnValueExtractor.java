package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDataset;

public enum DatasetColumnValueExtractor implements ColumnValueExtractor<ReportingDataset> {
  CREATION_TIME("creation_time", d -> toInsertRowString(d.getCreationTime())),
  CREATOR_ID("creator_id", ReportingDataset::getCreatorId),
  DATASET_ID("dataset_id", ReportingDataset::getDatasetId),
  DESCRIPTION("description", ReportingDataset::getDescription),
  INCLUDES_ALL_PARTICIPANTS(
      "includes_all_participants", ReportingDataset::isIncludesAllParticipants),
  LAST_MODIFIED_TIME("last_modified_time", d -> toInsertRowString(d.getLastModifiedTime())),
  NAME("name", ReportingDataset::getName),
  WORKSPACE_ID("workspace_id", ReportingDataset::getWorkspaceId);

  public static final String TABLE_NAME = "dataset";

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private final String parameterName;
  private final Function<ReportingDataset, Object> objectValueFunction;

  DatasetColumnValueExtractor(
      String parameterName, Function<ReportingDataset, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDataset, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
