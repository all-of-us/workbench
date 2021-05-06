package org.pmiops.workbench.api;

import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingService freeTierBillingService;
  private final BillingProjectBufferService billingProjectBufferService;

  @Autowired
  OfflineBillingController(
      FreeTierBillingService freeTierBillingService,
      BillingProjectBufferService billingProjectBufferService) {
    this.freeTierBillingService = freeTierBillingService;
    this.billingProjectBufferService = billingProjectBufferService;
  }

  @Override
  public ResponseEntity<Void> bufferBillingProjects() {
    billingProjectBufferService.bufferBillingProjects();
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
