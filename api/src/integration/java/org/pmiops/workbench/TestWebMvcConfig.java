package org.pmiops.workbench;

import org.pmiops.workbench.config.WebMvcConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestWebMvcConfig extends WebMvcConfig {

  @Bean
  @Primary
  public WorkbenchEnvironment workbenchEnvironment() {
    return new WorkbenchEnvironment(true, "appId");
  }
}
