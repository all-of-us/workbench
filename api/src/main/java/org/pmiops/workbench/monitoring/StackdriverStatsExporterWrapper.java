package org.pmiops.workbench.monitoring;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Simple wrapper service so we can mock out the initialization step (which depends directly on a
 * public static method)
 */
@Service
public class StackdriverStatsExporterWrapper {
  private static final Logger logger =
      Logger.getLogger(StackdriverStatsExporterWrapper.class.getName());
  private boolean initialized = false;

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   *
   * @param stackdriverStatsConfiguration
   */
  public void createAndRegister(StackdriverStatsConfiguration stackdriverStatsConfiguration) {
    if (!initialized) {
      try {
        StackdriverStatsExporter.createAndRegister(stackdriverStatsConfiguration);
        initialized = true;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
      }
    }
  }
}
