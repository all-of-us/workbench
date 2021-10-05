package org.pmiops.workbench.api;

import org.pmiops.workbench.billing.FreeTierBillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingService freeTierBillingService;

  @Autowired
  OfflineBillingController(FreeTierBillingService freeTierBillingService) {
    this.freeTierBillingService = freeTierBillingService;
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    freeTierBillingService.checkFreeTierBillingUsage();
    return ResponseEntity.noContent().build();
  }
}
