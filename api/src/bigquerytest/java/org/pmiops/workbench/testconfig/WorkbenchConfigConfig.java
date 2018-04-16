package org.pmiops.workbench.testconfig;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.CdrConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WorkbenchConfigConfig {

  @Bean
  public WorkbenchConfig workbenchConfig() {
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.cdr = new CdrConfig();
    workbenchConfig.cdr.debugQueries = true;
    return workbenchConfig;
  }
}
