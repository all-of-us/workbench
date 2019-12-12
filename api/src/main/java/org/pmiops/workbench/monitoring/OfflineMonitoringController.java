package org.pmiops.workbench.monitoring;

import java.util.Map;
import java.util.logging.Logger;
import org.pmiops.workbench.api.OfflineMonitoringApiDelegate;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineMonitoringController implements OfflineMonitoringApiDelegate {

  private static final Logger logger =
      Logger.getLogger(OfflineMonitoringController.class.getName());

  private GaugeRecorderService gaugeRecorderService;

  public OfflineMonitoringController(GaugeRecorderService gaugeRecorderService) {
    this.gaugeRecorderService = gaugeRecorderService;
  }

  @Override
  public ResponseEntity<Void> updateGaugeMetrics() {
    Map<OpenCensusStatsViewInfo, Number> viewToValue = gaugeRecorderService.record();
    logValues(viewToValue);
    return null;
  }

  private void logValues(Map<OpenCensusStatsViewInfo, Number> viewToValue) {
    StringBuilder textBuilder = new StringBuilder();
    viewToValue.forEach(
        (view, value) ->
            textBuilder.append(view.getStatsName()).append(" = ").append(value.toString()));
    logger.info(textBuilder.toString());
  }
}
