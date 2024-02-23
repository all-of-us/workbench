package org.pmiops.workbench.actionaudit;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import java.util.UUID;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ActionAuditSpringConfiguration {
  public static final String ACTION_ID_BEAN = "ACTION_ID";

  @Bean
  public Logging getCloudLogging() {
    return LoggingOptions.getDefaultInstance().getService();
  }

  @Bean(name = ACTION_ID_BEAN)
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public String getActionId() {
    return UUID.randomUUID().toString();
  }
}
