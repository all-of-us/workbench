package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineTestUsersController implements OfflineTestUsersApiDelegate {
  private static final Logger LOGGER = Logger.getLogger(OfflineTestUsersController.class.getName());

  public OfflineTestUsersController() {}

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspaces() {
    LOGGER.info("deleteAllTestUserWorkspaces is decommissioned...");
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteAllTestUserWorkspacesOrphanedInRawls() {
    LOGGER.info("deleteAllTestUserWorkspacesOrphanedInRawls is decommissioned...");
    return ResponseEntity.ok().build();
  }
}
