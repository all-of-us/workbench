package org.pmiops.workbench.monitoring;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.monitoring.views.StatsViewProperties;
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
  private StackdriverStatsExporterInitializationService
      stackdriverStatsExporterInitializationService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  MonitoringServiceStackdriverImpl(
      ViewManager viewManager,
      StatsRecorder statsRecorder,
      StackdriverStatsExporterInitializationService stackdriverStatsExporterInitializationService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.viewManager = viewManager;
    this.statsRecorder = statsRecorder;
    this.stackdriverStatsExporterInitializationService =
        stackdriverStatsExporterInitializationService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    registerSignals();
  }

  private void initStatsConfigurationIdempotent() {
    stackdriverStatsExporterInitializationService.createAndRegister(
        StackdriverStatsConfiguration.builder()
            .setMetricNamePrefix(buildMetricNamePrefix())
            .setProjectId(workbenchConfigProvider.get().server.projectId)
            .build());
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
  public void recordValue(StatsViewProperties viewProperties, Number value) {
    try {
      initStatsConfigurationIdempotent();
      if (value == null) {
        logger.log(
            Level.WARNING,
            String.format(
                "Attempting to log a null numeric value for signal %s", viewProperties.getName()));
        return;
      }
      if (viewProperties.getMeasureClass().equals(MeasureLong.class)) {
        recordLongValue(viewProperties.getMeasureLong(), value.longValue());
      } else if (viewProperties.getMeasureClass().equals(MeasureDouble.class)) {
        recordDoubleValue(viewProperties.getMeasureDouble(), value.doubleValue());
      } else {
        logger.log(
            Level.WARNING,
            String.format(
                "Unrecognized measure class %s", viewProperties.getMeasureClass().getName()));
      }
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  /**
   * Record multiple values at once, indexed by keys in the StatsViewProperties enum. This should
   * save calls to Stackdriver.
   *
   * @param enumToValue
   */
  @Override
  public void recordValue(Map<StatsViewProperties, Number> enumToValue) {
    try {
      final MeasureMap measureMap = statsRecorder.newMeasureMap();
      enumToValue.forEach((key, value) -> addToMeasureMap(measureMap, key, value));
      measureMap.record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void addToMeasureMap(
      MeasureMap measureMap, @NotNull StatsViewProperties viewProperties, Number value) {
    if (viewProperties.getMeasureClass().equals(MeasureLong.class)) {
      measureMap.put(viewProperties.getMeasureLong(), value.longValue());
    } else if (viewProperties.getMeasureClass().equals(MeasureDouble.class)) {
      measureMap.put(viewProperties.getMeasureDouble(), value.doubleValue());
    } else {
      logger.log(
          Level.WARNING,
          String.format(
              "Unrecognized measure class %s", viewProperties.getMeasureClass().getName()));
    }
  }

  private void recordLongValue(MeasureLong measureLong, Long value) {
    try {
      statsRecorder.newMeasureMap().put(measureLong, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void recordDoubleValue(MeasureDouble measureDouble, Double value) {
    try {
      statsRecorder.newMeasureMap().put(measureDouble, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, "Exception encountered during monitoring.", e);
  }
}
