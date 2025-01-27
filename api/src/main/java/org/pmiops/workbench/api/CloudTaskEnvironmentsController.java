package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceInaccessibleToSa;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoStatusUtils;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskEnvironmentsController implements CloudTaskEnvironmentsApiDelegate {
  public record WorkspaceWithUsers(DbWorkspace workspace, Set<String> usernames) {
    public boolean hasUsers() {
      return !usernames.isEmpty();
    }
  }

  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskEnvironmentsController.class.getName());

  private final LeonardoApiClient leonardoApiClient;
  private final Provider<Stopwatch> stopwatchProvider;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceService workspaceService;

  @Autowired
  public CloudTaskEnvironmentsController(
      LeonardoApiClient leonardoApiClient,
      Provider<Stopwatch> stopwatchProvider,
      WorkspaceDao workspaceDao,
      WorkspaceService workspaceService) {
    this.leonardoApiClient = leonardoApiClient;
    this.stopwatchProvider = stopwatchProvider;
    this.workspaceDao = workspaceDao;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Void> ctDeleteUnsharedWorkspaceEnvironments(
      List<String> workspaceNamespaces) {
    LOGGER.info(
        String.format(
            "Deleting unshared environments for %d workspaces.", workspaceNamespaces.size()));

    // temp timing for batch sizing
    var stopwatch = stopwatchProvider.get().start();

    var workspacesWithUsers =
        workspaceNamespaces.stream()
            .map(workspaceService::lookupWorkspaceByNamespace)
            .map(ws -> new WorkspaceWithUsers(ws, getCurrentUsernames(ws)))
            .toList();

    var failedUserRetrievals =
        workspacesWithUsers.stream()
            .filter(wu -> !wu.hasUsers())
            .map(WorkspaceWithUsers::workspace)
            .toList();

    workspacesWithUsers.stream().filter(WorkspaceWithUsers::hasUsers).forEach(this::deleteUnshared);

    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Attempted to delete unshared environments for %d workspaces (%d failed user retrievals) in %s.",
            workspaceNamespaces.size(),
            failedUserRetrievals.size(),
            formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }

  private void deleteUnshared(WorkspaceWithUsers workspaceAndUsernames) {
    var workspace = workspaceAndUsernames.workspace();
    var currentUsers = workspaceAndUsernames.usernames();

    var runtimes = leonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject());
    var runtimesToDelete =
        runtimes.stream()
            .filter(LeonardoStatusUtils::canDeleteRuntime)
            .filter(runtime -> !currentUsers.contains(runtime.getAuditInfo().getCreator()))
            .toList();

    var apps = leonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject());
    var appsToDelete =
        apps.stream()
            .filter(LeonardoStatusUtils::canDeleteApp)
            .filter(app -> !currentUsers.contains(app.getCreator()))
            .toList();

    if (!runtimesToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d runtimes for workspace %s/%s (ID %d) out of %d total",
              runtimesToDelete.size(),
              workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName(),
              workspace.getWorkspaceId(),
              runtimes.size()));

      runtimesToDelete.forEach(
          runtime -> {
            try {
              leonardoApiClient.deleteRuntimeAsService(
                  workspace.getGoogleProject(), runtime.getRuntimeName(), /* deleteDisk */ true);
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete runtime %s in project %s: %s",
                      runtime.getRuntimeName(), workspace.getGoogleProject(), e.getMessage()));
            }
          });
    }

    if (!appsToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d apps for workspace %s/%s (ID %d) out of %d total",
              appsToDelete.size(),
              workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName(),
              workspace.getWorkspaceId(),
              apps.size()));

      appsToDelete.forEach(
          app -> {
            try {
              leonardoApiClient.deleteAppAsService(
                  app.getAppName(), workspace, /* deleteDisk */ true);
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete app %s in project %s: %s",
                      app.getAppName(), workspace.getGoogleProject(), e.getMessage()));
            }
          });
    }

    // check disks after runtime and app deletion, because these will delete their associated disks

    var disks = leonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject());
    var disksToDelete =
        disks.stream()
            .filter(LeonardoStatusUtils::canDeleteDisk)
            .filter(disk -> !currentUsers.contains(disk.getAuditInfo().getCreator()))
            .toList();

    if (!disksToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d disks for workspace %s/%s (ID %d) out of %d total",
              disksToDelete.size(),
              workspace.getWorkspaceNamespace(),
              workspace.getFirecloudName(),
              workspace.getWorkspaceId(),
              disks.size()));

      disksToDelete.forEach(
          disk -> {
            try {
              leonardoApiClient.deletePersistentDiskAsService(
                  workspace.getGoogleProject(), disk.getName());
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete disk %s in project %s: %s",
                      disk.getName(), workspace.getGoogleProject(), e.getMessage()));
            }
          });
    }
  }

  // also creates a DbWorkspaceInaccessibleToSa record if appropriate
  private Set<String> getCurrentUsernames(DbWorkspace dbWorkspace) {
    try {
      return workspaceService
          .getFirecloudUserRoles(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
          .stream()
          .map(UserRole::getEmail)
          .collect(Collectors.toSet());
    } catch (WorkbenchException e) {
      LOGGER.warning(
          String.format(
              "Failed to get user roles for workspace %s/%s (ID %d).  Marking as inaccessible_to_sa: %s",
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              e.getMessage()));

      // this is not expected to be present, but check just in case, for DB integrity
      if (dbWorkspace.getWorkspaceInaccessibleToSa() == null) {
        var inaccessibleToSa =
            new DbWorkspaceInaccessibleToSa()
                .setWorkspace(dbWorkspace)
                .setNote("Failed to get user roles");
        workspaceDao.save(dbWorkspace.setWorkspaceInaccessibleToSa(inaccessibleToSa));
      }

      return Collections.emptySet();
    }
  }
}
