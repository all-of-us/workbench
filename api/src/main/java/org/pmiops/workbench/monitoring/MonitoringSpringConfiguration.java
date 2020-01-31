package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.util.UUID;
import java.util.logging.Level;
import javax.inject.Provider;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitoringSpringConfiguration {

  private static final String UNKNOWN_INSTANCE_PREFIX = "unknown-";
  private static final String MONITORED_RESOURCE_TYPE = "generic_node";
  private static final String PROJECT_ID_LABEL = "project_id";
  private static final String LOCATION_LABEL = "location";
  private static final String NAMESPACE_LABEL = "namespace";
  private static final String NODE_ID_LABEL = "node_id";

  public static final String APP_ENGINE_NODE_ID = "appEngineNodeId";

  @Bean
  public ViewManager getViewManager() {
    return Stats.getViewManager();
  }

  @Bean
  public StatsRecorder getStatsRecorder() {
    return Stats.getStatsRecorder();
  }

  @Bean
  public Tagger getTagger() {
    return Tags.getTagger();
  }

  @Bean(name = APP_ENGINE_NODE_ID)
  public String getAppEngineNodeId(ModulesService modulesService) {
    try {
      return modulesService.getCurrentInstanceId();
    } catch (ModulesException e) {
      return makeRandomNodeId();
    }
  }

  @Bean()
  public MonitoredResource getMonitoredResource(
      @Qualifier(CacheSpringConfiguration.WORKBENCH_CONFIG_SINGLETON) Provider<WorkbenchConfig> workbenchConfigProvider,
      @Qualifier(APP_ENGINE_NODE_ID) Provider<String> appEngineNodeIdProvider) {
    return MonitoredResource.newBuilder()
        .setType(MONITORED_RESOURCE_TYPE)
        .putLabels(PROJECT_ID_LABEL, workbenchConfigProvider.get().server.projectId)
        .putLabels(LOCATION_LABEL, workbenchConfigProvider.get().server.appEngineLocationId)
        .putLabels(NAMESPACE_LABEL, workbenchConfigProvider.get().server.shortName.toLowerCase())
        .putLabels(NODE_ID_LABEL, appEngineNodeIdProvider.get())
        .build();
  }

  /**
   * Stackdriver instances have very long, random ID strings. When running locally, however, the
   * ModulesService throws an exception, so we need to assign non-conflicting and non-repeating ID
   * strings.
   *
   * @return
   */
  private String makeRandomNodeId() {
    return UNKNOWN_INSTANCE_PREFIX + UUID.randomUUID().toString();
  }

}
