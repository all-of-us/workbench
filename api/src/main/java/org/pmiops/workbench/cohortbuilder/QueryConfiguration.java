package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;

public class QueryConfiguration {

  private final ImmutableList<ColumnInfo> selectColumns;
  private final QueryJobConfiguration queryJobConfiguration;

  public static class ColumnInfo {
    private final String columnName;
    private final ColumnConfig columnConfig;

    ColumnInfo(String columnName, ColumnConfig columnConfig) {
      this.columnName = columnName;
      this.columnConfig = columnConfig;
    }

    public String getColumnName() {
      return columnName;
    }

    public ColumnConfig getColumnConfig() {
      return columnConfig;
    }
  }

  public QueryConfiguration(ImmutableList<ColumnInfo> selectColumns,
      QueryJobConfiguration queryJobConfiguration) {
    this.selectColumns = selectColumns;
    this.queryJobConfiguration = queryJobConfiguration;
  }

  public ImmutableList<ColumnInfo> getSelectColumns() {
    return selectColumns;
  }

  public QueryJobConfiguration getQueryJobConfiguration() {
    return queryJobConfiguration;
  }
}
