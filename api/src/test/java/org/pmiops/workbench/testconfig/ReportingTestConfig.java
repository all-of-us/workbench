package org.pmiops.workbench.testconfig;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ReportingTestConfig {
  @Bean
  public WorkbenchConfig workbenchConfig() {
    final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.reporting.dataset = "wb_reporting";
    workbenchConfig.reporting.maxRowsPerInsert = 5;
    workbenchConfig.server.projectId = "rw-wb-unit-test";
    return workbenchConfig;
  }
}
