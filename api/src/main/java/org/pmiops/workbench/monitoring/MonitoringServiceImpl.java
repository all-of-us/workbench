package org.pmiops.workbench.monitoring;

import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringServiceImpl implements MonitoringService {
  private static final Logger logger = Logger.getLogger(MonitoringServiceImpl.class.getName());
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
    Arrays.stream(GaugeMetric.values())
        .map(Metric::toView)
        .forEach(viewManager::registerView);
  }

  @Override
  public void recordValues(
      Map<Metric, Number> viewInfoToValue,
      Map<String, AttachmentValue> attachmentKeyToValue) {
    try {
      initStatsConfigurationIdempotent();
      if (viewInfoToValue.isEmpty()) {
        logger.warning("recordValue() called with empty map.");
        return;
      }
      final MeasureMap measureMap = statsRecorder.newMeasureMap();
      viewInfoToValue.forEach(
          (viewProperties, value) -> addToMeasureMap(measureMap, viewProperties, value));
      attachmentKeyToValue.forEach(measureMap::putAttachment);

      // Finally, send the data to the backend (Stackdriver/Cloud Monitoring for now).
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
      @NotNull Metric viewProperties,
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
