package org.pmiops.workbench.access;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Provider;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbAccessService {
  private static final Logger log = Logger.getLogger(VwbAccessService.class.getName());

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final VwbUserService vwbUserService;

  // Cache for user existence checks to avoid repeated API calls
  // Entries expire after 30 seconds to balance between API call reduction and data freshness
  private final Cache<String, Boolean> userExistenceCache =
      CacheBuilder.newBuilder()
          .expireAfterWrite(30, TimeUnit.SECONDS)
          .maximumSize(1000) // Limit cache size to prevent unbounded growth
          .build();

  @Autowired
  public VwbAccessService(
      VwbUserManagerClient vwbUserManagerClient,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      VwbUserService vwbUserService) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.vwbUserService = vwbUserService;
  }

  /**
   * Adds a user to the Vwb tier group. Exceptions are logged and swallowed to allow the program to
   * proceed since the Vwb feature is still under development.
   *
   * @param userName the username of the user to be added
   * @param vwbGroupName the access tier to which the user is being added
   */
  public void addUserIntoVwbTier(DbUser user, String vwbGroupName) {
    if (workbenchConfigProvider.get().featureFlags.enableVWBUserAccessManagement) {
      if (!checkUserExists(user.getUsername())) {
        log.info(String.format("User %s does not exist in VWB skipping...'", user.getUsername()));
        return;
      }
      log.info(
          String.format("Added user %s to VWB Tier group '%s'", user.getUsername(), vwbGroupName));
      vwbUserManagerClient.addUserToGroup(vwbGroupName, user.getUsername());
    }
  }

  /**
   * Checks if a user exists in VWB, using a cache to avoid repeated API calls.
   *
   * @param username the username to check
   * @return true if the user exists in VWB, false otherwise
   */
  private boolean checkUserExists(String username) {
    try {
      // Try to get from cache first, compute if absent
      return userExistenceCache.get(username, () -> vwbUserService.doesUserExist(username));
    } catch (Exception e) {
      // If cache operation fails, fall back to direct call
      log.warning(
          "Cache operation failed for user "
              + username
              + ", falling back to direct call: "
              + e.getMessage());
      return vwbUserService.doesUserExist(username);
    }
  }

  /**
   * Removes a user from the Vwb tier group. Exceptions are logged and swallowed to allow the
   * program to proceed since the Vwb feature is still under development.
   *
   * @param user the DbUser of the user to be removed
   * @param vwbGroupName the access tier from which the user is being removed
   */
  public void removeUserFromVwbTier(DbUser user, String vwbGroupName) {
    if (workbenchConfigProvider.get().featureFlags.enableVWBUserAccessManagement) {
      if (user.getVwbUserPod() == null) {
        log.info(String.format("User %s does not exist in VWB skipping...'", user.getUsername()));
        return;
      }
      log.info(
          String.format(
              "Remove user %s from VWB Tier group '%s'", user.getUsername(), vwbGroupName));
      vwbUserManagerClient.removeUserFromGroup(vwbGroupName, user.getUsername());
    }
  }
}
