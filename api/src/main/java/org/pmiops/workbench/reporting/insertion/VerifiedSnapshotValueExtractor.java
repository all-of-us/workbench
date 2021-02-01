package org.pmiops.workbench.reporting.insertion;

import org.pmiops.workbench.model.ReportingVerifiedSnapshot;

public enum VerifiedSnapshotValueExtractor
    implements ColumnValueExtractor<ReportingVerifiedSnapshot> {

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private static final String TABLE_NAME = "verifiedSnapshot";
  private final String parameterName;
  private final Function<ReportingVerifiedSnapshot, Object> objectValueFunction;

  VerifiedSnapshotValueExtractor(
      String parameterName, Function<ReportingVerifiedSnapshot, Object> objectValueFunction) {
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
  public Function<ReportingVerifiedSnapshot, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
