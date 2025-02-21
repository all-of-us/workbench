package org.pmiops.workbench.api;

import static org.pmiops.workbench.cloudtasks.TaskQueueService.tmpDiskCheck;
import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskAdminService;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoStatusUtils;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskEnvironmentsController implements CloudTaskEnvironmentsApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskEnvironmentsController.class.getName());

  private final DiskAdminService diskAdminService;
  private final LeonardoApiClient leonardoApiClient;
  private final Provider<Stopwatch> stopwatchProvider;
  private final WorkspaceService workspaceService;

  @Autowired
  public CloudTaskEnvironmentsController(
      DiskAdminService diskAdminService,
      LeonardoApiClient leonardoApiClient,
      Provider<Stopwatch> stopwatchProvider,
      WorkspaceService workspaceService) {
    this.diskAdminService = diskAdminService;
    this.leonardoApiClient = leonardoApiClient;
    this.stopwatchProvider = stopwatchProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Void> deleteUnsharedWorkspaceEnvironmentsBatch(
      List<String> workspaceNamespaces) {
    LOGGER.info(
        String.format(
            "Deleting unshared environments for %d workspaces.", workspaceNamespaces.size()));

    var stopwatch = stopwatchProvider.get().start();
    long failedUserRoles =
        workspaceService.lookupWorkspacesByNamespace(workspaceNamespaces).stream()
            .filter(
                ws -> {
                  boolean successfulGetFirecloudUserRoles = deleteUnshared(ws);
                  return !successfulGetFirecloudUserRoles;
                })
            .count();
    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Attempted to delete unshared environments for %d workspaces (%d failures) in %s.",
            workspaceNamespaces.size(), failedUserRoles, formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> checkPersistentDisksBatch(List<Disk> persistentDiskBatch) {
    LOGGER.info(String.format("Checking %s persistent disks.", persistentDiskBatch.size()));

    tmpDiskCheck(persistentDiskBatch);

    var stopwatch = stopwatchProvider.get().start();
    diskAdminService.checkPersistentDisks(persistentDiskBatch);
    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Completed checking %s persistent disks in %s.",
            persistentDiskBatch.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }

  // return success value of getFirecloudUserRoles()
  private boolean deleteUnshared(DbWorkspace dbWorkspace) {
    // this call often fails on Test.  TODO: investigate.
    final List<UserRole> userRoles;
    try {
      userRoles =
          workspaceService.getFirecloudUserRoles(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
    } catch (WorkbenchException e) {
      LOGGER.warning(
          String.format(
              "Failed to get user roles for workspace %s/%s (ID %d): %s",
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              e.getMessage()));
      return false;
    }

    var currentUsers = userRoles.stream().map(UserRole::getEmail).collect(Collectors.toSet());

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
    return true;
  }
}
