package org.pmiops.workbench.monitoring;

import io.opencensus.metrics.data.AttachmentValue;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.monitoring.views.OpenCensusView;
import org.pmiops.workbench.monitoring.views.ViewProperties;
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
    Arrays.stream(ViewProperties.values())
        .map(ViewProperties::toView)
        .forEach(viewManager::registerView);
  }

  /**
   * Record multiple values at once, indexed by keys in the StatsViewProperties enum. This should
   * save calls to Stackdriver.
   *
   * @param viewInfoToValue
   */
  @Override
  public void recordValues(Map<OpenCensusView, Number> viewInfoToValue) {
    recordValues(viewInfoToValue, Collections.emptyMap());
  }

  @Override
  public void recordValues(
      Map<OpenCensusView, Number> viewInfoToValue,
      Map<String, AttachmentValue> attachmentKeysToValue) {
    try {
      initStatsConfigurationIdempotent();
      if (viewInfoToValue.isEmpty()) {
        logger.warning("recordValue() called with empty map.");
        return;
      }
      final MeasureMap measureMap = statsRecorder.newMeasureMap();
      viewInfoToValue.forEach(
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
      @NotNull OpenCensusView viewProperties,
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

  private void logAndSwallow(RuntimeException e) {
    logger.log(Level.WARNING, "Exception encountered during monitoring.", e);
  }
}
