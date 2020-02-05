package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.OfflineBillingApiDelegate;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingService freeTierBillingService;
  private final BillingProjectBufferService billingProjectBufferService;
  private final BillingGarbageCollectionService billingGarbageCollectionService;
  private LogsBasedMetricService logsBasedMetricService;

  @Autowired
  OfflineBillingController(
      FreeTierBillingService freeTierBillingService,
      BillingProjectBufferService billingProjectBufferService,
      BillingGarbageCollectionService billingGarbageCollectionService,
      LogsBasedMetricService logsBasedMetricService) {
    this.freeTierBillingService = freeTierBillingService;
    this.billingProjectBufferService = billingProjectBufferService;
    this.billingGarbageCollectionService = billingGarbageCollectionService;
    this.logsBasedMetricService = logsBasedMetricService;
  }

  @Override
  public ResponseEntity<Void> billingProjectGarbageCollection() {
    billingGarbageCollectionService.deletedWorkspaceGarbageCollection();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> bufferBillingProjects() {
    billingProjectBufferService.bufferBillingProjects();
    // TODO(jaycarlton) we're going to need a richer framework for cron reporting,
    // possibly from the client side. This is just an example to make sure labels work.
    // It would be massively awkward to put a try/catch around every job just for error reporting.
    logsBasedMetricService.record(
        MeasurementBundle.builder()
            .addEvent(EventMetric.CRON_JOB_END)
            .addTag(MetricLabel.CRON_JOB_NAME, "bufferBillingProjects")
            .addTag(MetricLabel.CRON_JOB_SUCCEEDED, Boolean.valueOf(true).toString())
            .build());
    // TODO(jaycarlton): record completion time as a distribution metric
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> syncBillingProjectStatus() {
    billingProjectBufferService.syncBillingProjectStatus();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> cleanBillingBuffer() {
    billingProjectBufferService.cleanBillingBuffer();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    freeTierBillingService.checkFreeTierBillingUsage();
    return ResponseEntity.noContent().build();
  }
}
