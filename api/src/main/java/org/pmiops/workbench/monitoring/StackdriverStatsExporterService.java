package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import com.google.common.annotations.VisibleForTesting;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Simple wrapper service so we can mock out the initialization step (which depends directly on a
 * public static method)
 */
@Service
public class StackdriverStatsExporterService {

  private static final Logger logger =
      Logger.getLogger(StackdriverStatsExporterService.class.getName());
  private static final String STACKDRIVER_CUSTOM_METRICS_PREFIX = "custom.googleapis.com/";
  private static final String MONITORED_RESOURCE_TYPE = "generic_node";
  private static final String PROJECT_ID_LABEL = "project_id";
  private static final String LOCATION_LABEL = "location";
  private static final String NAMESPACE_LABEL = "namespace";
  private static final String NODE_ID_LABEL = "node_id";
  private static final String UNKNOWN_INSTANCE_PREFIX = "unknown-";

  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private Provider<MonitoredResource> monitoredResourceProvider;

  public StackdriverStatsExporterService(
      @Qualifier(CacheSpringConfiguration.WORKBENCH_CONFIG_SINGLETON) Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<MonitoredResource> monitoredResourceProvider) {
    this.monitoredResourceProvider = monitoredResourceProvider;
    logger.warning("Making StackdriverStatsExporterService");
    this.workbenchConfigProvider = workbenchConfigProvider;
    createAndRegister();
  }

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   */
  private void createAndRegister() {
    try {
      final StackdriverStatsConfiguration configuration = makeStackdriverStatsConfiguration();
      StackdriverStatsExporter.createAndRegister(configuration);
      logger.info(
          String.format(
              "Configured StackDriver exports with configuration:\n%s",
              configuration.toString()));
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
    }
  }

  @VisibleForTesting
  public StackdriverStatsConfiguration makeStackdriverStatsConfiguration() {
    return StackdriverStatsConfiguration.builder()
        .setMetricNamePrefix(STACKDRIVER_CUSTOM_METRICS_PREFIX)
        .setProjectId(workbenchConfigProvider.get().server.projectId)
        .setMonitoredResource(monitoredResourceProvider.get())
        .build();
  }
}
