package org.pmiops.workbench.monitoring;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.monitoring.signals.MonitoringViews;
import org.pmiops.workbench.monitoring.signals.StatsViewProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringServiceStackdriverImpl implements MonitoringService {
  private static final Logger logger =
      Logger.getLogger(MonitoringServiceStackdriverImpl.class.getName());
  private static final String DOMAIN_NAME = "custom.googleapis.com";
  private static final String ORG_NAME = "org.pmiops.workbench";
  private ViewManager viewManager;
  private StatsRecorder statsRecorder;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  MonitoringServiceStackdriverImpl(
      ViewManager viewManager,
      StatsRecorder statsRecorder,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.viewManager = viewManager;
    this.statsRecorder = statsRecorder;
    this.workbenchConfigProvider = workbenchConfigProvider;
    createAndRegisterStatsConfiguration();
  }

  private void createAndRegisterStatsConfiguration() {
    try {
      // TODO(jaycarlton) I'd really like to be able to use project & env values
      // from the config, but those aren't available in constructors. Maybe we lazily initialize
      // somehow?
      StackdriverStatsExporter.createAndRegister(
          StackdriverStatsConfiguration.builder()
              //              .setMetricNamePrefix(buildMetricNamePrefix())
              //              .setProjectId(workbenchConfigProvider.get().server.projectId)
              .build());
      registerSignals();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failure to initialize stackdriver stats exporter");
    }
  }

  private String buildMetricNamePrefix() {
    return String.format(
        "%s/%s/%s/",
        DOMAIN_NAME, ORG_NAME, workbenchConfigProvider.get().server.shortName.toLowerCase());
  }

  private void registerSignals() {
    Arrays.stream(MonitoringViews.values())
        .map(MonitoringViews::toStatsView)
        .forEach(viewManager::registerView);
  }

  @Override
  public void send(StatsViewProperties signal, Object value) {
    if (value == null) {
      logger.log(
          Level.WARNING,
          String.format("Attempting to log a null numeric value for signal %s", signal.getName()));
      return;
    }
    if (signal.getMeasureClass().equals(MeasureLong.class)) {
      recordLongValue(signal.getMeasureLong(), (long) value);
    } else if (signal.getMeasureClass().equals(MeasureDouble.class)) {
      recordDoubleValue(signal.getMeasureDouble(), (double) value);
    } else {
      logger.log(
          Level.WARNING,
          String.format("Unrecognized measure class %s", signal.getMeasureClass().getName()));
    }
  }

  private void recordLongValue(MeasureLong measureLong, Long value) {
    try {
      statsRecorder.newMeasureMap().put(measureLong, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void recordDoubleValue(MeasureDouble measurementInformation, Double value) {
    try {
      statsRecorder.newMeasureMap().put(measurementInformation, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, "Exception encountered during monitoring.", e);
  }
}
