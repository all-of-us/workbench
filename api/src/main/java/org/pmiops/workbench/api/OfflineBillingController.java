package org.pmiops.workbench.api;

import org.pmiops.workbench.billing.FreeTierBillingBatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingBatchUpdateService freeTierBillingService;

  @Autowired
  OfflineBillingController(FreeTierBillingBatchUpdateService freeTierBillingService) {
    this.freeTierBillingService = freeTierBillingService;
  }

  @Override
  public ResponseEntity<Void> checkFreeTierBillingUsage() {
    freeTierBillingService.checkFreeTierBillingUsage();
    return ResponseEntity.noContent().build();
  }
}
