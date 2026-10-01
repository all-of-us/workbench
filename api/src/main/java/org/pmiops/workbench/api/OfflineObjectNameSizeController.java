package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
/** Controller to audit users who created objects in their buckets with very long names */
public class OfflineObjectNameSizeController implements OfflineObjectNameSizeApiDelegate {

  private static final Logger logger =
      Logger.getLogger(OfflineObjectNameSizeController.class.getName());

  public OfflineObjectNameSizeController() {}

  public ResponseEntity<Void> checkObjectNameSize() {
    logger.info("checkObjectNameSize is decommissioned");
    return ResponseEntity.noContent().build();
  }
}
