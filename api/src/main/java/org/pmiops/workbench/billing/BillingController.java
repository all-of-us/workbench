package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.BillingApiDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BillingController implements BillingApiDelegate {

  private final BillingAlertsService billingAlertsService;
  private final BillingProjectBufferService billingProjectBufferService;

  @Autowired
  BillingController(
      BillingAlertsService billingAlertsService,
      BillingProjectBufferService billingProjectBufferService) {
    this.billingAlertsService = billingAlertsService;
    this.billingProjectBufferService = billingProjectBufferService;
  }

  @Override
  public ResponseEntity<Void> bufferBillingProject() {
    billingProjectBufferService.bufferBillingProject();
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Override
  public ResponseEntity<Void> syncBillingProjectStatus() {
    billingProjectBufferService.syncBillingProjectStatus();
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Override
  public ResponseEntity<Void> cleanBillingBuffer() {
    billingProjectBufferService.cleanBillingBuffer();
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    billingAlertsService.checkFreeTierBillingUsage();
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
