package org.pmiops.workbench.monitoring;

import org.springframework.stereotype.Service;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  @Override
  public void registerSignal(
      String signalName,
      String signalDescription,
      Measure measurementInformation,
      Aggregation aggregation) {
    View view =
        View.create(
            View.Name.create(signalName),
            signalDescription,
            measurementInformation,
            aggregation,
            Collections.emptyList());
    ViewManager viewManager = Stats.getViewManager();
    viewManager.registerView(view);
  }

  @Override
  public void sendLongSignal(Measure.MeasureLong measurementInformation, Long longValue) {
    STATS_RECORDER.newMeasureMap().put(measurementInformation, longValue).record();
  }

  @Override
  public void sendDoubleSignal(Measure.MeasureDouble measurementInformation, Double doubleValue) {
    STATS_RECORDER.newMeasureMap().put(measurementInformation, doubleValue).record();
  }
}
