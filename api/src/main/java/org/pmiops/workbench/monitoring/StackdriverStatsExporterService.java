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
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.stereotype.Service;

/**
 * Simple wrapper service so we can mock out the initialization step (which depends directly on a
 * public static method)
 */
@Service
public class StackdriverStatsExporterService {

  private static final Logger logger =
      Logger.getLogger(StackdriverStatsExporterService.class.getName());
  private static final String STACKDRIVER_CUSTOM_METRICS_DOMAIN_NAME = "custom.googleapis.com";
  private static final String MONITORED_RESOURCE_TYPE = "generic_node";
  private static final String PROJECT_ID_LABEL = "project_id";
  private static final String LOCATION_LABEL = "location";
  private static final String NAMESPACE_LABEL = "namespace";
  private static final String NODE_ID_LABEL = "node_id";

  private boolean initialized;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private ModulesService modulesService;

  public StackdriverStatsExporterService(
      Provider<WorkbenchConfig> workbenchConfigProvider, ModulesService modulesService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.modulesService = modulesService;
    this.initialized = false;
  }

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   */
  public void createAndRegister() {
    if (!initialized) {
      try {
        final StackdriverStatsConfiguration configuration =
            makeStackdriverStatsConfiguration();
        StackdriverStatsExporter.createAndRegister(configuration);
        logger.info(
            String.format(
                "Configured StackDriver exports with configuration:\n%s",
                configuration.toString()));
        initialized = true;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
      }
    }
  }

  @VisibleForTesting
  public StackdriverStatsConfiguration makeStackdriverStatsConfiguration() {
    return StackdriverStatsConfiguration.builder()
        .setMetricNamePrefix(buildMetricNamePrefix())
        .setProjectId(getProjectId())
        .setMonitoredResource(makeMonitoredResource())
        .build();
  }

  private MonitoredResource makeMonitoredResource() {
    final MonitoredResource.Builder resultBuilder =
        MonitoredResource.newBuilder()
            .setType(MONITORED_RESOURCE_TYPE)
            .putLabels(PROJECT_ID_LABEL, getProjectId())
            .putLabels(LOCATION_LABEL, getLocation())
            .putLabels(NAMESPACE_LABEL, getEnvironmentShortName())
            .putLabels(NODE_ID_LABEL, getNodeId());

    return resultBuilder.build();
  }

  private String getProjectId() {
    return workbenchConfigProvider.get().server.projectId;
  }

  private String getLocation() {
    return workbenchConfigProvider.get().server.appEngineLocationId;
  }

  private String getNodeId() {
    try {
      return modulesService.getCurrentInstanceId();
    } catch (ModulesException e) {
      logger.log(Level.INFO, "Failed to retrieve instance ID from ModulesService");
      return makeRandomNodeId();
    }
  }

  private String makeRandomNodeId() {
    return UUID.randomUUID().toString();
  }

  private String buildMetricNamePrefix() {
    return String.format(
        "%s/%s/", STACKDRIVER_CUSTOM_METRICS_DOMAIN_NAME, getEnvironmentShortName());
  }

  @NotNull
  private String getEnvironmentShortName() {
    return workbenchConfigProvider.get().server.shortName.toLowerCase();
  }
}
