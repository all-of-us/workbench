package org.pmiops.workbench.access;

import jakarta.inject.Provider;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbAccessService {
  private static final Logger log = Logger.getLogger(VwbAccessService.class.getName());

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public VwbAccessService(
      VwbUserManagerClient vwbUserManagerClient,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
      if (user.getVwbUserPod() == null) {
        log.info(String.format("User %s does not exist in VWB skipping...'", user.getUsername()));
        return;
      }
      log.info(
          String.format("Added user %s to VWB Tier group '%s'", user.getUsername(), vwbGroupName));
      vwbUserManagerClient.addUserToGroup(vwbGroupName, user.getUsername());
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
