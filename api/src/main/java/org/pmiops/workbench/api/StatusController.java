package org.pmiops.workbench.api;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.StatusResponse;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private MonitoringService monitoringService;

  @Autowired
  StatusController(
      FireCloudService fireCloudService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      MonitoringService monitoringService) {
    this.fireCloudService = fireCloudService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.monitoringService = monitoringService;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonardoNotebooksClient.getNotebooksStatus());
    sendDistribution();
    sendEvents();
    return ResponseEntity.ok(statusResponse);
  }

  private void sendEvents() {
    IntStream.rangeClosed(0, 99)
        .forEach(i ->  monitoringService.recordEvent(EventMetric.NOTEBOOK_CLONE));
  }

  private void sendDistribution() {
    final Random random = new Random();
    Stream.generate(random::nextDouble)
        .limit(100)
        .collect(Collectors.toList())
        .forEach(v -> monitoringService.recordValue(DistributionMetric.RANDOM_SAMPLE, v));
  }
}
