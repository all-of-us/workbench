package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingLeonardoAppUsage;

/*
 * Column data and metadata convertors for BigQuery user_general_discovery_source table in reporting
 * dataset.
 */
public enum LeonardoAppUsageColumnValueExtractor
    implements ColumnValueExtractor<ReportingLeonardoAppUsage> {
  APP_ID("app_id", ReportingLeonardoAppUsage::getAppId),
  APP_NAME("app_name", ReportingLeonardoAppUsage::getAppName),
  STATUS("status", ReportingLeonardoAppUsage::getStatus),
  CREATOR("creator", ReportingLeonardoAppUsage::getCreator),
  START_TIME("start_time", v -> toInsertRowString(v.getStartTime())),
  STOP_TIME("stop_time", v -> toInsertRowString(v.getStopTime())),
  CREATED_DATE("created_date", v -> toInsertRowString(v.getCreatedDate())),
  DESTROYED_DATE("destroyed_date", v -> toInsertRowString(v.getDestroyedDate())),
  ENVIRONMENT_VARIABLES(
      "environment_variables", ReportingLeonardoAppUsage::getEnvironmentVariables);

  public static final String TABLE_NAME = "leonardo_app_usage";

  private final String parameterName;
  private final Function<ReportingLeonardoAppUsage, Object> rowToInsertValueFunction;

  LeonardoAppUsageColumnValueExtractor(
      String parameterName, Function<ReportingLeonardoAppUsage, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingLeonardoAppUsage, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
