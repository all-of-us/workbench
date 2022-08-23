package org.pmiops.workbench.api;

import java.util.LinkedList;
import java.util.List;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.ObjectNameSizeService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineObjectNameSizeController implements OfflineObjectNameSizeApiDelegate {

  @Autowired UserService userService;
  @Autowired WorkspaceService workspaceService;
  @Autowired ObjectNameSizeService objectNameSizeService;

  public ResponseEntity<Void> checkObjectNameSize() {

    for (DbUser user : userService.getAllUsers()) {
      List<DbWorkspace> offendingWorkspacesForUser = new LinkedList<>();
      for (DbWorkspace workspace : workspaceService.getActiveWorkspacesForUser(user)) {
        if (objectNameSizeService.objectNameSizeExceedsThreshold(workspace)) {
          offendingWorkspacesForUser.add(workspace);
        }
      }

      if (!offendingWorkspacesForUser.isEmpty()) {
        // Create a jira ticket.

      }
    }

    return ResponseEntity.noContent().build();
  }
}
