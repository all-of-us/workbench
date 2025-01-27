package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
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

    var workspacesAndUsernames =
        workspaceNamespaces.stream()
            .map(workspaceService::lookupWorkspaceByNamespace)
            .map(ws -> Pair.of(ws, getCurrentUsernames(ws)))
            .toList();

    var failedUserRoles =
        workspacesAndUsernames.stream()
            .filter(p -> p.getRight().isEmpty())
            .map(Pair::getLeft)
            .toList();

    workspacesAndUsernames.stream()
        .filter(p -> !p.getRight().isEmpty())
        .forEach(this::deleteUnshared);

    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Attempted to delete unshared environments for %d workspaces (%d failures) in %s.",
            workspaceNamespaces.size(), failedUserRoles.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }

  private void deleteUnshared(Pair<DbWorkspace, Set<String>> workspaceAndUsernames) {
    var dbWorkspace = workspaceAndUsernames.getLeft();
    var currentUsers = workspaceAndUsernames.getRight();

    var runtimes = leonardoApiClient.listRuntimesByProjectAsService(dbWorkspace.getGoogleProject());
    var runtimesToDelete =
        runtimes.stream()
            .filter(LeonardoStatusUtils::canDeleteRuntime)
            .filter(runtime -> !currentUsers.contains(runtime.getAuditInfo().getCreator()))
            .toList();

    var apps = leonardoApiClient.listAppsInProjectAsService(dbWorkspace.getGoogleProject());
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
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              runtimes.size()));

      runtimesToDelete.forEach(
          runtime -> {
            try {
              leonardoApiClient.deleteRuntimeAsService(
                  dbWorkspace.getGoogleProject(), runtime.getRuntimeName(), /*
             deleteDisk */ true);
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete runtime %s in project %s: %s",
                      runtime.getRuntimeName(), dbWorkspace.getGoogleProject(), e.getMessage()));
            }
          });
    }

    if (!appsToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d apps for workspace %s/%s (ID %d) out of %d total",
              appsToDelete.size(),
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              apps.size()));

      appsToDelete.forEach(
          app -> {
            try {
              leonardoApiClient.deleteAppAsService(
                  app.getAppName(), dbWorkspace, /* deleteDisk */ true);
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete app %s in project %s: %s",
                      app.getAppName(), dbWorkspace.getGoogleProject(), e.getMessage()));
            }
          });
    }

    // check disks after runtime and app deletion, because these will delete their associated disks

    var disks = leonardoApiClient.listDisksByProjectAsService(dbWorkspace.getGoogleProject());
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
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              disks.size()));

      disksToDelete.forEach(
          disk -> {
            try {
              leonardoApiClient.deletePersistentDiskAsService(
                  dbWorkspace.getGoogleProject(), disk.getName());
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete disk %s in project %s: %s",
                      disk.getName(), dbWorkspace.getGoogleProject(), e.getMessage()));
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

      workspaceDao.save(
          dbWorkspace.setWorkspaceInaccessibleToSa(
              new DbWorkspaceInaccessibleToSa()
                  .setWorkspace(dbWorkspace)
                  .setNote("Failed to get user roles")));

      return Collections.emptySet();
    }
  }
}
