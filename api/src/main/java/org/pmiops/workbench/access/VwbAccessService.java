package org.pmiops.workbench.access;

import jakarta.inject.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
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
  public void addUserIntoVwbTier(String userName, String vwbGroupName) {
    if (workbenchConfigProvider.get().featureFlags.enableVWBUserAccessManagement) {
      // First, try with VWB UM API first.
      try {
        vwbUserManagerClient.addUserToGroup(vwbGroupName, userName);
      } catch (Exception e) {
        log.log(Level.WARNING, "Failed to add user to Vwb tier group using UM" + e.getMessage());
      }
    }
  }

  /**
   * Removes a user from the Vwb tier group. Exceptions are logged and swallowed to allow the
   * program to proceed since the Vwb feature is still under development.
   *
   * @param userName the username of the user to be removed
   * @param vwbGroupName the access tier from which the user is being removed
   */
  public void removeUserFromVwbTier(String userName, String vwbGroupName) {
    if (workbenchConfigProvider.get().featureFlags.enableVWBUserAccessManagement) {
      // First, try with VWB UM API first.
      try {
        vwbUserManagerClient.removeUserFromGroup(vwbGroupName, userName);
      } catch (Exception e) {
        log.log(Level.WARNING, "Failed to add user to Vwb tier group using UM" + e.getMessage());
      }
    }
  }
}
