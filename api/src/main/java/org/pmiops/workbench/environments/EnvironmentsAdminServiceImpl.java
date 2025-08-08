package org.pmiops.workbench.environments;


import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoStatusUtils;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentsAdminServiceImpl implements EnvironmentsAdminService {
  private static final Logger LOGGER =
      Logger.getLogger(EnvironmentsAdminServiceImpl.class.getName());

  private final WorkspaceService workspaceService;
  private final LeonardoApiClient leonardoApiClient;
  private final WorkspaceUserCacheService workspaceUserCacheService;

  @Autowired
  public EnvironmentsAdminServiceImpl(
      WorkspaceService workspaceService,
      LeonardoApiClient leonardoApiClient,
      WorkspaceUserCacheService workspaceUserCacheService
      ) {
    this.workspaceService = workspaceService;
    this.leonardoApiClient = leonardoApiClient;
    this.workspaceUserCacheService = workspaceUserCacheService;
  }

  /** * Delete unshared environments (runtimes, apps, and disks) in the given workspaces. This method
   * will return the number of workspaces that failed to fetch workspace ACLs.
   *
   * @param workspaceNamespaces the namespaces of the workspaces to check for unshared environments
   * @return the number of workspaces that failed to fetch workspace ACLs
   */
  @Override
  public long deleteUnsharedWorkspaceEnvironmentsBatch(List<String> workspaceNamespaces) {
    return workspaceService.lookupWorkspacesByNamespace(workspaceNamespaces).stream()
            .filter(
                ws -> {
                  boolean successfulGetFirecloudUserRoles = deleteUnshared(ws);
                  return !successfulGetFirecloudUserRoles;
                })
            .count();
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

  /**
   * Given a list of deletable runtimes and the current users with access to the workspace, delete
   * any runtimes not owned by a current user.
   *
   * @param dbWorkspace workspace the runtimes are in
   * @param deletableRuntimes all runtimes in the workspace that are in a deletable state
   * @param currentUsers the current users with access to the workspace
   * @param totalRuntimes the total number of runtimes in the workspace
   */
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

  /**
   * Given a list of deletable apps and the current users with access to the workspace, delete
   * any apps not owned by a current user.
   *
   * @param dbWorkspace workspace the apps are in
   * @param deletableApps all apps in the workspace that are in a deletable state
   * @param currentUsers the current users with access to the workspace
   * @param totalApps the total number of apps in the workspace
   */
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

  /**
   * Check for unshared disks in the given workspace and delete any that are not owned by a current
   * user. If we have any disks to delete, and we haven't already refetched current users, do so
   * now.
   *
   * @param dbWorkspace the workspace to check for unshared disks
   * @param cachedUsers the users with access to the workspace according to the workspace user cache
   * @param currentUsers the users with access to the workspace according to a live call to Rawls,
   *     or null if we haven't made that call yet
   * @return true if we were able to get current users from Rawls if needed, false if that call
   *     failed
   */
  private boolean deleteUnsharedDisks(
      DbWorkspace dbWorkspace, Set<String> cachedUsers, @Nullable Set<String> currentUsers) {
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

  /**
   * Given a list of deletable disks and the current users with access to the workspace, delete any
   * disks not owned by a current user.
   *
   * @param dbWorkspace workspace the disks are in
   * @param deletableDisks all disks in the workspace that are in a deletable state
   * @param currentUsers the current users with access to the workspace
   * @param totalDisks the total number of disks in the workspace
   */
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

  /**
   * Make a live call to Rawls to get the current users with access to the workspace. Called when
   * the workspace user cache indicates that there may be resources to delete as we need to get
   * fresh ACLs before deleting anything.
   * @param dbWorkspace the workspace to get current users for
   * @return the set of usernames for users with access to the workspace
   */
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
