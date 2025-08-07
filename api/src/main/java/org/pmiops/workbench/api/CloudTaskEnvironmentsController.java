package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskAdminService;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoStatusUtils;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
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
  private final WorkspaceUserCacheService workspaceUserCacheService;

  @Autowired
  public CloudTaskEnvironmentsController(
      DiskAdminService diskAdminService,
      LeonardoApiClient leonardoApiClient,
      Provider<Stopwatch> stopwatchProvider,
      WorkspaceService workspaceService,
      WorkspaceUserCacheService workspaceUserCacheService) {
    this.diskAdminService = diskAdminService;
    this.leonardoApiClient = leonardoApiClient;
    this.stopwatchProvider = stopwatchProvider;
    this.workspaceService = workspaceService;
    this.workspaceUserCacheService = workspaceUserCacheService;
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

    var stopwatch = stopwatchProvider.get().start();
    diskAdminService.checkPersistentDisks(persistentDiskBatch);
    var elapsed = stopwatch.stop().elapsed();
    LOGGER.info(
        String.format(
            "Completed checking %s persistent disks in %s.",
            persistentDiskBatch.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.ok().build();
  }

  /**
   * Delete runtimes, apps, and disks in the given workspace that are not owned by any user with
   * access to the workspace. In most cases, we have nothing to delete. If we find any potential
   * resources to delete, we refetch the current workspace ACL before deleting in case the cache is
   * stale. We delete runtimes and apps before disks, since deleting a runtime or app will also
   * delete its associated disk.
   *
   * <p>The overall flow is: 1. Get workspace users out of cache 2. Fetch runtimes and apps from
   * Leonardo 3. Find runtime and app deletion candidates 4. If we have any runtime or app
   * candidates, refetch current workspace users 5. Confirm that runtime or app candidates should
   * still be deleted based on current ACL 6. Delete runtime or app candidates 7. Fetch disks from
   * Leonardo 8. Find disk deletion candidates 9. If we have any disk candidates, and we didn't
   * refetch workspace current users, do so now 10. If we have any disk candidates, and we did
   * refetch workspace current users, use those 11. Confirm that disk candidates should still be
   * deleted based on current ACL 12. Delete candidates
   *
   * @param dbWorkspace the workspace to check for unshared environments and disks
   * @return true if we were able to get current users from Rawls, false if that call failed
   */
  private boolean deleteUnshared(DbWorkspace dbWorkspace) {
    // this call often fails on Test.  TODO: investigate.

    // get users out of cache
    var cachedUsers = workspaceUserCacheService.getWorkspaceUsers(dbWorkspace.getWorkspaceId());

    // check for runtimes and apps to delete
    var runtimes = leonardoApiClient.listRuntimesByProjectAsService(dbWorkspace.getGoogleProject());
    var deletableRuntimes =
        runtimes.stream().filter(LeonardoStatusUtils::canDeleteRuntime).toList();
    var possibleRuntimesToDelete =
        deletableRuntimes.stream()
            .filter(runtime -> !cachedUsers.contains(runtime.getAuditInfo().getCreator()))
            .toList();

    var apps = leonardoApiClient.listAppsInProjectAsService(dbWorkspace.getGoogleProject());
    var deletableApps = apps.stream().filter(LeonardoStatusUtils::canDeleteApp).toList();
    var possibleAppsToDelete =
        deletableApps.stream().filter(app -> !cachedUsers.contains(app.getCreator())).toList();

    // if we have any apps or runtimes to delete, refetch current users before trying to delete
    final Set<String> currentUsers;
    if (!possibleRuntimesToDelete.isEmpty() || !possibleAppsToDelete.isEmpty()) {
      try {
        currentUsers = getCurrentUsers(dbWorkspace);
      } catch (WorkbenchException e) {
        return false;
      }

      // maybe delete runtimes and apps
      maybeDeleteRuntimes(dbWorkspace, deletableRuntimes, currentUsers, runtimes.size());
      maybeDeleteApps(dbWorkspace, deletableApps, currentUsers, apps.size());
    } else {
      currentUsers = null;
    }

    return deleteUnsharedDisks(dbWorkspace, cachedUsers, currentUsers);
  }

  private void maybeDeleteRuntimes(
      DbWorkspace dbWorkspace,
      List<LeonardoListRuntimeResponse> deletableRuntimes,
      Set<String> currentUsers,
      Integer totalRuntimes) {
    var runtimesToDelete =
        deletableRuntimes.stream()
            .filter(runtime -> !currentUsers.contains(runtime.getAuditInfo().getCreator()))
            .toList();

    if (!runtimesToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d runtimes for workspace %s/%s (ID %d) out of %d total",
              runtimesToDelete.size(),
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              totalRuntimes));
      runtimesToDelete.forEach(
          runtime -> {
            try {
              leonardoApiClient.deleteRuntimeAsService(
                  dbWorkspace.getGoogleProject(), runtime.getRuntimeName(), /* deleteDisk */ true);
            } catch (WorkbenchException e) {
              LOGGER.warning(
                  String.format(
                      "Failed to delete runtime %s in project %s: %s",
                      runtime.getRuntimeName(), dbWorkspace.getGoogleProject(), e.getMessage()));
            }
          });
    }
  }

  private void maybeDeleteApps(
      DbWorkspace dbWorkspace,
      List<UserAppEnvironment> deletableApps,
      Set<String> currentUsers,
      Integer totalApps) {
    var appsToDelete =
        deletableApps.stream().filter(app -> !currentUsers.contains(app.getCreator())).toList();

    if (!appsToDelete.isEmpty()) {
      LOGGER.info(
          String.format(
              "Deleting %d apps for workspace %s/%s (ID %d) out of %d total",
              appsToDelete.size(),
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              totalApps));
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
  }

  private boolean deleteUnsharedDisks(
      DbWorkspace dbWorkspace, Set<String> cachedUsers, Set<String> currentUsers) {
    // check for disks to delete
    // (check disks after runtime and app deletion, because these will delete their associated
    // disks)
    var disks = leonardoApiClient.listDisksByProjectAsService(dbWorkspace.getGoogleProject());
    var deletableDisks = disks.stream().filter(LeonardoStatusUtils::canDeleteDisk).toList();
    var potentialDisksToDelete =
        deletableDisks.stream()
            .filter(disk -> !cachedUsers.contains(disk.getAuditInfo().getCreator()))
            .toList();

    // if we have any to delete, and we haven't refetched current users, do so now
    // if we have any to delete, and we've already refetched current users, then use that
    if (!potentialDisksToDelete.isEmpty()) {
      final Set<String> diskCurrentUsers;
      if (currentUsers == null) {
        try {
          diskCurrentUsers = getCurrentUsers(dbWorkspace);
        } catch (WorkbenchException e) {
          return false;
        }
      } else {
        diskCurrentUsers = currentUsers;
      }

      maybeDeleteDisks(dbWorkspace, deletableDisks, diskCurrentUsers, disks.size());
    }

    return true;
  }

  private void maybeDeleteDisks(
      DbWorkspace dbWorkspace,
      List<ListPersistentDiskResponse> deletableDisks,
      Set<String> currentUsers,
      Integer totalDisks) {
    var disksToDelete =
        deletableDisks.stream()
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
              totalDisks));
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

  private Set<String> getCurrentUsers(DbWorkspace dbWorkspace) {
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
              "Failed to get user roles for workspace %s/%s (ID %d): %s",
              dbWorkspace.getWorkspaceNamespace(),
              dbWorkspace.getFirecloudName(),
              dbWorkspace.getWorkspaceId(),
              e.getMessage()));
      throw e;
    }
  }
}
