package org.pmiops.workbench.api;

import org.pmiops.workbench.monitoring.GaugeRecorderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineMonitoringController implements OfflineMonitoringApiDelegate {

  private GaugeRecorderService gaugeRecorderService;

  public OfflineMonitoringController(GaugeRecorderService gaugeRecorderService) {
    this.gaugeRecorderService = gaugeRecorderService;
  }

  @Override
  public ResponseEntity<Void> updateGaugeMetrics() {
    gaugeRecorderService.record();
    return null;
  }
}
