package org.pmiops.workbench.audit;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionAuditSpringConfiguration {

  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  ActionAuditSpringConfiguration(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Bean
  public Logging getCloudLogging() {
    return LoggingOptions.getDefaultInstance().getService();
  }

  @Bean
  MonitoredResource getMonitoredResource() {
    final String resourceName = configProvider.get().actionAudit.monitoredResourceName;
    return MonitoredResource.newBuilder(resourceName).build();
  }
}
