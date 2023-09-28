package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;

/*
 * Column data and metadata convertors for BigQuery user_general_discovery_source table in reporting
 * dataset.
 */
public enum UserGeneralDiscoverySourceColumnValueExtractor
    implements ColumnValueExtractor<ReportingUserGeneralDiscoverySource> {
  USER_ID("user_id", ReportingUserGeneralDiscoverySource::getUserId),
  ANSWER("answer", ReportingUserGeneralDiscoverySource::getAnswer),
  OTHER_TEXT("otherText", ReportingUserGeneralDiscoverySource::getOtherText);

  public static final String TABLE_NAME = "user_general_discovery_source";

  private final String parameterName;
  private final Function<ReportingUserGeneralDiscoverySource, Object> rowToInsertValueFunction;

  UserGeneralDiscoverySourceColumnValueExtractor(
      String parameterName,
      Function<ReportingUserGeneralDiscoverySource, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingUserGeneralDiscoverySource, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
