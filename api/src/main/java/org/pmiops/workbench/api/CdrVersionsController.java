package org.pmiops.workbench.api;

import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.model.CdrVersionTiersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdrVersionsController implements CdrVersionsApiDelegate {
  private final CdrVersionService cdrVersionService;

  @Autowired
  CdrVersionsController(CdrVersionService cdrVersionService) {
    this.cdrVersionService = cdrVersionService;
  }

  @Override
  public ResponseEntity<CdrVersionTiersResponse> getCdrVersionsByTier() {
    return ResponseEntity.ok(cdrVersionService.getCdrVersionsByTier());
  }
}
