package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;

/*
 * Column data and metadata convertors for BigQuery workspace_free_tier_usage table in reporting
 * dataset.
 */
public enum WorkspaceFreeTierUsageColumnValueExtractor
    implements ColumnValueExtractor<ReportingWorkspaceFreeTierUsage> {
  COST("cost", ReportingWorkspaceFreeTierUsage::getCost),
  USER_ID("user_id", ReportingWorkspaceFreeTierUsage::getUserId),
  WORKSPACE_ID("workspace_id", ReportingWorkspaceFreeTierUsage::getWorkspaceId);

  private static final String TABLE_NAME = "workspace_free_tier_usage";
  private final String parameterName;
  private final Function<ReportingWorkspaceFreeTierUsage, Object> rowToInsertValueFunction;

  WorkspaceFreeTierUsageColumnValueExtractor(
      String parameterName,
      Function<ReportingWorkspaceFreeTierUsage, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
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
  public Function<ReportingWorkspaceFreeTierUsage, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
