package org.pmiops.workbench.testconfig;

import org.pmiops.workbench.config.WorkbenchConfig;

public class TestWorkbenchConfig {

  public static class BigQueryConfig {
    public String dataSetId;
    public String projectId;
  }

  public BigQueryConfig bigquery;
}
