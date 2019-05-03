package org.pmiops.workbench.billing;

import org.pmiops.workbench.api.BillingApiDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
public class BillingController implements BillingApiDelegate {

  private static final Logger log = Logger.getLogger(BillingController.class.getName());

  private final BillingProjectBufferService billingProjectBufferService;

  @Autowired
  BillingController(BillingProjectBufferService billingProjectBufferService) {
    this.billingProjectBufferService = billingProjectBufferService;
  }

  @Override
  public ResponseEntity<Void> bufferBillingProject() {
    billingProjectBufferService.bufferBillingProject();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}