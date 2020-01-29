package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import java.io.IOException;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class StackdriverMonitoringSpringConfiguration {

  private static final String MONITORED_RESOURCE_TYPE = "generic_node";
  private static final String PROJECT_ID_LABEL = "project_id";
  private static final String LOCATION_LABEL = "location";
  private static final String NAMESPACE_LABEL = "namespace";
  private static final String NODE_ID_LABEL = "node_id";
  private static final String UNKNOWN_INSTANCE_PREFIX = "unknown-";

  public static final String APP_ENGINE_NODE_ID = "APP_ENGINE_NODE_ID";

  @Bean
  public MetricServiceClient getMetricServiceClient() throws IOException {
    return MetricServiceClient.create();
  }

  /**
   * Provide a  MonitoredResource object for Stackdriver Monitoring. (Additionally, this could
   * be used for tracing and logging as well, though we should tread carefully there.
   *
   * This method was moved of an OpenCensus-specific service for use by both the Cloud Monitoring API-
   * based monitoring service implementation and the OC-based one.
   */
  @Bean
  @Scope("prototype")
  public MonitoredResource getMonitoredResource(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      @Qualifier(APP_ENGINE_NODE_ID) Provider<String> nodeIdProvider) {
    return MonitoredResource.newBuilder()
        .setType(MONITORED_RESOURCE_TYPE)
        .putLabels(PROJECT_ID_LABEL, workbenchConfigProvider.get().server.projectId)
        .putLabels(LOCATION_LABEL, workbenchConfigProvider.get().server.appEngineLocationId)
        .putLabels(NAMESPACE_LABEL, workbenchConfigProvider.get().server.shortName.toLowerCase())
        .putLabels(NODE_ID_LABEL, nodeIdProvider.get())
        .build();
  }

  /**
   * Stackdriver instances have very long, random ID strings. When running locally, however, the
   * ModulesService throws an exception, so we need to assign non-conflicting and non-repeating ID
   * strings.
   *
   * @return - node ID string.
   */
  @Bean(name = APP_ENGINE_NODE_ID)
  public String getAppEngineNodeId(ModulesService modulesService) {
    try {
      return modulesService.getCurrentInstanceId();
    } catch (ModulesException e) {
      return UNKNOWN_INSTANCE_PREFIX + UUID.randomUUID().toString();
    }
  }
}
