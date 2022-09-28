package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.pmiops.workbench.exfiltration.ObjectNameLengthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
/** Controller to audit users who created objects in their buckets with very long names */
public class OfflineObjectNameSizeController implements OfflineObjectNameSizeApiDelegate {

  private static final Logger LOGGER =
      Logger.getLogger(OfflineObjectNameSizeController.class.getName());

  private final ObjectNameLengthService objectNameLengthService;

  @Autowired
  public OfflineObjectNameSizeController(ObjectNameLengthService objectNameLengthService) {
    this.objectNameLengthService = objectNameLengthService;
  }

  public ResponseEntity<Void> checkObjectNameSize() {
    LOGGER.info("Starting checking object lengths audit job");

    objectNameLengthService.calculateObjectNameLength();

    LOGGER.info("Finished checking object lengths audit job");
    return ResponseEntity.noContent().build();
  }
}
