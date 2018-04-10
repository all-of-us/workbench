package org.pmiops.workbench.cohortbuilder;

import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.model.TableQuery;

public class TableQueryAndConfig {

  private final TableQuery tableQuery;
  private final CdrBigQuerySchemaConfig config;

  public TableQueryAndConfig(TableQuery tableQuery, CdrBigQuerySchemaConfig config) {
    this.tableQuery = tableQuery;
    this.config = config;
  }

  public TableQuery getTableQuery() {
    return tableQuery;
  }

  public CdrBigQuerySchemaConfig getConfig() {
    return config;
  }
}
