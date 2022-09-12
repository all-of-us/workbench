package org.pmiops.workbench.api;

import java.time.Clock;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ObjectNameSizeService;
import org.pmiops.workbench.exfiltration.jirahandler.EgressJiraHandler;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineObjectNameSizeController implements OfflineObjectNameSizeApiDelegate {

  private static final Logger LOGGER =
      Logger.getLogger(OfflineObjectNameSizeController.class.getName());
  private static final long THRESHOLD = 100;

  private final UserService userService;
  private final WorkspaceService workspaceService;
  private final ObjectNameSizeService objectNameSizeService;
  private final EgressEventDao egressEventDao;
  private final EgressRemediationService egressRemediationService;

  @Autowired
  public OfflineObjectNameSizeController(
      Clock clock,
      UserService userService,
      WorkspaceService workspaceService,
      ObjectNameSizeService objectNameSizeService,
      EgressEventDao egressEventDao,
      @Qualifier("internal-jira-handler") EgressJiraHandler egressJiraHandler,
      @Qualifier("internal-remediation-service")
          EgressRemediationService egressRemediationService) {
    this.userService = userService;
    this.workspaceService = workspaceService;
    this.objectNameSizeService = objectNameSizeService;
    this.egressEventDao = egressEventDao;
    this.egressRemediationService = egressRemediationService;
  }

  public ResponseEntity<Void> checkObjectNameSize() {

    for (DbUser user : userService.getAllUsers()) {
      List<DbWorkspace> offendingWorkspacesForUser = new LinkedList<>();
      for (DbWorkspace workspace : workspaceService.getActiveWorkspacesForUser(user)) {
        long nameLength = objectNameSizeService.calculateObjectNameLength(workspace);
        if (nameLength > THRESHOLD) {

          LOGGER.info(
              String.format(
                  "An offending workspaces has been found for workspace ID: %d, user ID: %s",
                  workspace.getWorkspaceId(), user.getUserId()));

          offendingWorkspacesForUser.add(workspace);

          // Create an egress alert.
          Optional<DbEgressEvent> maybeEvent =
              this.maybePersistEgressEvent(nameLength, workspace.getCreator(), workspace);
          egressRemediationService.remediateEgressEvent(maybeEvent.get().getEgressEventId());
        }
      }
    }

    return ResponseEntity.noContent().build();
  }

  private Optional<DbEgressEvent> maybePersistEgressEvent(
      Long objectNameLengths, DbUser dbUser, DbWorkspace dbWorkspace) {

    return Optional.of(
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(dbUser)
                .setWorkspace(dbWorkspace)
                .setEgressMegabytes(
                    Optional.ofNullable(objectNameLengths)
                        // bytes -> Megabytes (10^6 bytes)
                        .map(bytes -> (float) (bytes / (1024 * 1024)))
                        .orElse(null))
                .setStatus(DbEgressEventStatus.PENDING)));
  }
}
