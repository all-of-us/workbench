package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.OfflineBillingApiDelegate;
import org.pmiops.workbench.model.BillingProjectGarbageCollectionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineBillingController implements OfflineBillingApiDelegate {

  private final BillingAlertsService billingAlertsService;
  private final BillingProjectBufferService billingProjectBufferService;
  private final BillingGarbageCollectionService billingGarbageCollectionService;

  @Autowired
  OfflineBillingController(
      BillingAlertsService billingAlertsService,
      BillingProjectBufferService billingProjectBufferService,
      BillingGarbageCollectionService billingGarbageCollectionService) {
    this.billingAlertsService = billingAlertsService;
    this.billingProjectBufferService = billingProjectBufferService;
    this.billingGarbageCollectionService = billingGarbageCollectionService;
  }

  @Override
  public ResponseEntity<BillingProjectGarbageCollectionResponse> billingProjectGarbageCollection() {
    BillingProjectGarbageCollectionResponse response =
        billingGarbageCollectionService.deletedWorkspaceGarbageCollection();
    return ResponseEntity.ok(response);
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
