package org.pmiops.workbench.audit;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionAuditSpringConfiguration {

  @Bean
  public Logging getCloudLogging() {
    return LoggingOptions.getDefaultInstance().getService();
  }
}
