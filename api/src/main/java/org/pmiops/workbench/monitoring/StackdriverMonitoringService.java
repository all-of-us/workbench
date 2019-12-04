package org.pmiops.workbench.monitoring;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pmiops.workbench.monitoring.signals.Signal;
import org.springframework.stereotype.Service;

@Service
public class StackdriverMonitoringService implements MonitoringService {
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();
  private static final Logger log = Logger.getLogger(StackdriverMonitoringService.class.getName());

  StackdriverMonitoringService() {
    try {
      StackdriverStatsExporter.createAndRegister();
    } catch (IOException e) {
      log.log(Level.SEVERE, "Failure to initialize stackdriver stats exporter");
    }
  }

  // signalName should be unique, if you change the aggregation type or the measure type, it will
  // barf and fail. Fix by
  // changing the name or using the stackdriver monitoring API here:
  // https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.metricDescriptors/delete to
  // delete and recreate.
  @Override
  public void registerSignal(
      Signal signal) {
    View view =
        View.create(
            View.Name.create(signal.getName()),
            signal.getDescription(),
            signal.getMeasure(),
            signal.getAggregation(),
            Collections.emptyList());
    ViewManager viewManager = Stats.getViewManager();
    viewManager.registerView(view);
  }

  @Override
  public void sendSignal(Signal signal, Object value) {
    if (signal.getMeasureClass().equals(Measure.MeasureLong.class)) {
      sendLongSignal(signal.getMeasureLong(), (long) value);
    } else if (signal.getMeasureClass().equals(Measure.MeasureDouble.class)) {
      sendDoubleSignal(signal.getMeasureDouble(), (double) value);
    }
  }

  public void sendLongSignal(Measure.MeasureLong measurementInformation, Long value) {
    try {
      STATS_RECORDER.newMeasureMap().put(measurementInformation, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  public void sendDoubleSignal(Measure.MeasureDouble measurementInformation, Double value) {
    try {
      STATS_RECORDER.newMeasureMap().put(measurementInformation, value).record();
    } catch (RuntimeException e) {
      logAndSwallow(e);
    }
  }

  private void logAndSwallow(RuntimeException e) {
    log.log(Level.SEVERE, String.format("Exception encountered during monitoring.", e));
  }
}
