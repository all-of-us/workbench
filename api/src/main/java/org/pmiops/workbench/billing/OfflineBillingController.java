package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.OfflineBillingApiDelegate;
import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final FreeTierBillingService freeTierBillingService;
  private final BillingProjectBufferService billingProjectBufferService;
  private final BillingGarbageCollectionService billingGarbageCollectionService;

  @Autowired
  OfflineBillingController(
      FreeTierBillingService freeTierBillingService,
      BillingProjectBufferService billingProjectBufferService,
      BillingGarbageCollectionService billingGarbageCollectionService) {
    this.freeTierBillingService = freeTierBillingService;
    this.billingProjectBufferService = billingProjectBufferService;
    this.billingGarbageCollectionService = billingGarbageCollectionService;
  }

  @Override
  public ResponseEntity<Void> billingProjectGarbageCollection() {
    billingGarbageCollectionService.deletedWorkspaceGarbageCollection();
    return ResponseEntity.noContent().build();
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

  @Override
  public ResponseEntity<BillingProjectBufferStatus> getBillingProjectBufferStatus() {
    return ResponseEntity.ok(billingProjectBufferService.getStatus());
  }
}
