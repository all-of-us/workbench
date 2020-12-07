package org.pmiops.workbench.api;

import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.model.BillingProjectBufferStatusByTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitoringController implements MonitoringApiDelegate {
  private final BillingProjectBufferService billingProjectBufferService;

  @Autowired
  MonitoringController(BillingProjectBufferService billingProjectBufferService) {
    this.billingProjectBufferService = billingProjectBufferService;
  }

  @Override
  public ResponseEntity<BillingProjectBufferStatus> getBillingProjectBufferStatus() {
    return ResponseEntity.ok(billingProjectBufferService.getStatus());
  }

  @Override
  public ResponseEntity<BillingProjectBufferStatusByTier> getBillingProjectBufferStatusByTier() {
    return ResponseEntity.ok(billingProjectBufferService.getStatusByTier());
  }
}
