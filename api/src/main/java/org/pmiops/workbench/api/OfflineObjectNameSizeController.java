package org.pmiops.workbench.api;

import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.ObjectNameLengthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineObjectNameSizeController implements OfflineObjectNameSizeApiDelegate {

  private static final Logger LOGGER =
      Logger.getLogger(OfflineObjectNameSizeController.class.getName());

  private final WorkspaceService workspaceService;
  private final ObjectNameLengthService objectNameLengthService;

  @Autowired
  public OfflineObjectNameSizeController(
      WorkspaceService workspaceService, ObjectNameLengthService objectNameLengthService) {
    this.workspaceService = workspaceService;
    this.objectNameLengthService = objectNameLengthService;
  }

  public ResponseEntity<Void> checkObjectNameSize() {

    LOGGER.info("Starting checking object lengths audit job");

    final Set<DbWorkspace> activeWorkspaces = workspaceService.getActiveWorkspaces();
    LOGGER.info(String.format("There are %d active workspaces", activeWorkspaces.size()));

    for (DbWorkspace workspace : activeWorkspaces) {
      objectNameLengthService.calculateObjectNameLength(workspace);
    }

    LOGGER.info("Finished checking object lengths audit job");
    return ResponseEntity.noContent().build();
  }
}
