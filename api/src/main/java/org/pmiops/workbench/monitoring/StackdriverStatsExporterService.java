package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.common.annotations.VisibleForTesting;
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
  private static final String STACKDRIVER_CUSTOM_METRICS_PREFIX = "custom.googleapis.com/";

  private MonitoredResource monitoredResource;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  public StackdriverStatsExporterService(
      MonitoredResource monitoredResource, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.monitoredResource = monitoredResource;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   */
  public void createAndRegister() {
    try {
      final StackdriverStatsConfiguration configuration = makeStackdriverStatsConfiguration();
      StackdriverStatsExporter.createAndRegister(configuration);
      logger.info(
          String.format(
              "Configured StackDriver exports with configuration:\n%s", configuration.toString()));
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
    }
  }

  @VisibleForTesting
  public StackdriverStatsConfiguration makeStackdriverStatsConfiguration() {
    return StackdriverStatsConfiguration.builder()
        .setMetricNamePrefix(STACKDRIVER_CUSTOM_METRICS_PREFIX)
        .setProjectId(workbenchConfigProvider.get().server.projectId)
        .setMonitoredResource(monitoredResource)
        .build();
  }
}
