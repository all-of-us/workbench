package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;

public class QueryConfiguration {

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

  private final ImmutableList<ColumnInfo> selectColumns;
  private final QueryJobConfiguration queryJobConfiguration;

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
