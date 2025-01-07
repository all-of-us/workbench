package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbWorkspace;
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
  private final WorkspaceService workspaceService;

  @Autowired
  public CloudTaskEnvironmentsController(
      LeonardoApiClient leonardoApiClient, WorkspaceService workspaceService) {
    this.leonardoApiClient = leonardoApiClient;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Void> ctDeleteUnsharedWorkspaceEnvironments(
      List<String> workspaceNamespaces) {
    LOGGER.info(
        String.format(
            "Deleting unshared environments for %d workspaces.", workspaceNamespaces.size()));

    workspaceNamespaces.stream()
        .map(workspaceService::lookupWorkspaceByNamespace)
        .forEach(
            ws -> {
              //              explore(ws);
              deleteUnshared(ws);
            });

    return ResponseEntity.ok().build();
  }

  private void explore(DbWorkspace dbWorkspace) {
    LOGGER.info(
        String.format(
            "Exploring workspace %s/%s (ID %d)",
            dbWorkspace.getWorkspaceNamespace(),
            dbWorkspace.getFirecloudName(),
            dbWorkspace.getWorkspaceId()));

    var roles =
        workspaceService.getFirecloudUserRoles(
            dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    roles.forEach(
        ur -> LOGGER.info(String.format("Role %s for user %s", ur.getRole(), ur.getEmail())));

    var runtimes = leonardoApiClient.listRuntimesByProjectAsService(dbWorkspace.getGoogleProject());
    runtimes.forEach(
        runtime ->
            LOGGER.info(
                String.format(
                    "Runtime %s created by user %s",
                    runtime.getRuntimeName(), runtime.getAuditInfo().getCreator())));

    var apps = leonardoApiClient.listAppsInProjectAsService(dbWorkspace.getGoogleProject());
    apps.forEach(
        app ->
            LOGGER.info(
                String.format(
                    "App %s (%s) created by user %s",
                    app.getAppName(), app.getAppType(), app.getCreator())));

    var disks = leonardoApiClient.listDisksByProjectAsService(dbWorkspace.getGoogleProject());
    disks.forEach(
        disk ->
            LOGGER.info(
                String.format(
                    "Disk %s created by user %s",
                    disk.getName(), disk.getAuditInfo().getCreator())));
  }

  private void deleteUnshared(DbWorkspace dbWorkspace) {
    var currentUsers =
        workspaceService
            .getFirecloudUserRoles(
                dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
            .stream()
            .map(UserRole::getEmail)
            .collect(Collectors.toSet());

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
}
