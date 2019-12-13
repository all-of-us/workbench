package org.pmiops.workbench.monitoring;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
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

  private boolean initialized;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  public StackdriverStatsExporterService(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.initialized = false;
  }

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   */
  public void createAndRegister() {
    if (!initialized) {
      try {
        StackdriverStatsExporter.createAndRegister(StackdriverStatsConfiguration.builder()
            .setMetricNamePrefix(buildMetricNamePrefix())
            .setProjectId(workbenchConfigProvider.get().server.projectId)
            .build());
        initialized = true;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
      }
    }
  }

  private String buildMetricNamePrefix() {
    return String.format(
        "%s/%s/",
        STACKDRIVER_CUSTOM_METRICS_DOMAIN_NAME,
        workbenchConfigProvider.get().server.shortName.toLowerCase());
  }

}
