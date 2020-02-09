package org.pmiops.workbench.monitoring;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
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
  private Tagger tagger;

  @Autowired
  MonitoringServiceImpl(
      ViewManager viewManager,
      StatsRecorder statsRecorder,
      StackdriverStatsExporterService stackdriverStatsExporterService,
      Tagger tagger) {
    this.viewManager = viewManager;
    this.statsRecorder = statsRecorder;
    this.stackdriverStatsExporterService = stackdriverStatsExporterService;
    this.tagger = tagger;
  }

  private void initStatsConfigurationIdempotent() {
    if (!viewsAreRegistered) {
      registerMetricViews();
      viewsAreRegistered = true;
    }
    stackdriverStatsExporterService.createAndRegister();
  }

  /**
   * We don't want to register LogsBasedMetric enum values with OpenCensus. It makes it confusing ot
   * see custom.googleapis.com/metric_name in addition to logging/user/metric_name.
   * TODO(jaycarlton): restore event metrics here once OpenCensus is working
   */
  private void registerMetricViews() {
    StreamSupport.stream(
            Iterables.concat(Arrays.<Metric>asList(GaugeMetric.values())).spliterator(), false)
        .map(Metric::toView)
        .forEach(viewManager::registerView);
  }

  @Override
  public void recordValues(Map<Metric, Number> metricToValue, Map<TagKey, TagValue> tags) {
    initStatsConfigurationIdempotent();
    if (metricToValue.isEmpty()) {
      logger.warning("recordValue() called with empty map.");
      return;
    }
    final MeasureMap measureMap = statsRecorder.newMeasureMap();
    metricToValue.forEach((metric, value) -> addToMeasureMap(measureMap, metric, value));

    final TagContextBuilder tagContextBuilder = tagger.currentBuilder();
    tags.forEach(tagContextBuilder::putLocal);

    // Finally, send the data to the backend (Stackdriver/Cloud Monitoring for now).
    measureMap.record(tagContextBuilder.build());
    logger.info(String.format("Record measurements: %s, tags: %s", metricToValue, tags));
  }

  /**
   * Insert either a MeasureLong or MeasureDouble with appropriate value into the appropriate
   * MeasureMap.
   *
   * @param measureMap map to be populated
   * @param metric properties that will give a view
   * @param value
   */
  private void addToMeasureMap(
      @NotNull MeasureMap measureMap, @NotNull Metric metric, @NotNull Number value) {
    if (metric.getMeasureClass().equals(MeasureLong.class)) {
      measureMap.put(metric.getMeasureLong(), value.longValue());
    } else if (metric.getMeasureClass().equals(MeasureDouble.class)) {
      measureMap.put(metric.getMeasureDouble(), value.doubleValue());
    } else {
      logger.log(
          Level.WARNING,
          String.format("Unrecognized measure class %s", metric.getMeasureClass().getName()));
    }
  }
}
