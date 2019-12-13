package org.pmiops.workbench.monitoring;

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
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringServiceImpl implements MonitoringService {
  private static final Logger logger =
      Logger.getLogger(MonitoringServiceImpl.class.getName());
  private boolean viewsAreRegistered = false;
  private ViewManager viewManager;
  private StatsRecorder statsRecorder;
  private StackdriverStatsExporterService stackdriverStatsExporterService;

  @Autowired
  MonitoringServiceImpl(
      ViewManager viewManager,
      StatsRecorder statsRecorder,
      StackdriverStatsExporterService stackdriverStatsExporterService) {
    this.viewManager = viewManager;
    this.statsRecorder = statsRecorder;
    this.stackdriverStatsExporterService = stackdriverStatsExporterService;
  }

  private void initStatsConfigurationIdempotent() {
    if (!viewsAreRegistered) {
      registerSignals();
      viewsAreRegistered = true;
    }
    stackdriverStatsExporterService.createAndRegister();
  }

  private void registerSignals() {
    Arrays.stream(MonitoringViews.values())
        .map(MonitoringViews::toOpenCensusView)
        .forEach(viewManager::registerView);
  }

  @Override
  public void recordValue(OpenCensusStatsViewInfo viewInfo, Number value) {
    try {
      initStatsConfigurationIdempotent();
      if (value == null) {
        logger.log(
            Level.WARNING,
            String.format(
                "Attempting to log a null numeric value for signal %s", viewInfo.getName()));
        return;
      }
      if (viewInfo.getMeasureClass().equals(MeasureLong.class)) {
        recordLongValue(viewInfo.getMeasureLong(), value.longValue());
      } else if (viewInfo.getMeasureClass().equals(MeasureDouble.class)) {
        recordDoubleValue(viewInfo.getMeasureDouble(), value.doubleValue());
      } else {
        logger.log(
            Level.WARNING,
            String.format(
                "Unrecognized measure class %s", viewInfo.getMeasureClass().getName()));
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
  public void recordValues(Map<OpenCensusStatsViewInfo, Number> enumToValue) {
    try {
      initStatsConfigurationIdempotent();
      if (enumToValue.isEmpty()) {
        logger.warning("recordValue() called with empty map.");
        return;
      }
      final MeasureMap measureMap = statsRecorder.newMeasureMap();
      enumToValue.forEach(
          (viewProperties, value) -> addToMeasureMap(measureMap, viewProperties, value));
      measureMap.record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  /**
   * Insert either a MeasureLong or MeasureDouble with appropriate value into the appropriate
   * MeasureMap.
   *
   * @param measureMap map to be populated
   * @param viewProperties properties that will give a view
   * @param value
   */
  private void addToMeasureMap(
      @NotNull MeasureMap measureMap,
      @NotNull OpenCensusStatsViewInfo viewProperties,
      @NotNull Number value) {
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
