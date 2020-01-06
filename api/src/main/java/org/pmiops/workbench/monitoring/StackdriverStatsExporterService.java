package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.swing.text.html.Option;
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
  public static final String UNKNOWN_LOCATION = "global";

  private boolean initialized;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private ModulesService modulesService;

  public StackdriverStatsExporterService(Provider<WorkbenchConfig> workbenchConfigProvider,
      ModulesService modulesService) {
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
        final String projectId = workbenchConfigProvider.get().server.projectId;
        final StackdriverStatsConfiguration configuration =
            StackdriverStatsConfiguration.builder()
                .setMetricNamePrefix(buildMetricNamePrefix())
                .setProjectId(projectId)
                .setMonitoredResource(makeMonitoredResource(projectId))
                .build();
        StackdriverStatsExporter.createAndRegister(configuration);
        logger.info(String.format("Configured StackDriver exports with configuration:\n%s",
            configuration.toString()));
        initialized = true;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
      }
    }
  }

  private Optional<String> getOptionalFromThrowable(Supplier<String> extractor) {
    Optional<String> result = Optional.empty();
    try {
      result = Optional.of(extractor.get());
    } catch (RuntimeException e) {
      logger.log(Level.INFO, "Failed to retrieve String ", e);
    }
    return result;
  }

  private MonitoredResource makeMonitoredResource(String projectId) {
    final MonitoredResource.Builder resultBuilder = MonitoredResource.newBuilder()
        .setType(MONITORED_RESOURCE_TYPE)
        .putLabels(PROJECT_ID_LABEL, projectId)
        .putLabels(LOCATION_LABEL, UNKNOWN_LOCATION);
    getCurrentModule().ifPresent(m -> resultBuilder.putLabels(NAMESPACE_LABEL, m));
    getInstanceId().ifPresent(id -> resultBuilder.putLabels(NODE_ID_LABEL, id));

    return resultBuilder.build();
  }

  private Optional<String> getCurrentModule() {
    return getOptionalFromThrowable(modulesService::getCurrentModule);
//    Optional<String> result = Optional.empty();
//    try {
//      result = Optional.of(modulesService.getCurrentModule());
//    } catch (IllegalStateException e) {
//      logger.log(Level.INFO, "AppEngine Module not available", e);
//    }
//    return result;
  }

  private Optional<String> getInstanceId() {
    return getOptionalFromThrowable(modulesService::getCurrentInstanceId);
  }

  private String buildMetricNamePrefix() {
    return String.format(
        "%s/%s/",
        STACKDRIVER_CUSTOM_METRICS_DOMAIN_NAME,
        workbenchConfigProvider.get().server.shortName.toLowerCase());
  }
}
