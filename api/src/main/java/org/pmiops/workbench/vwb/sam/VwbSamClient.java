package org.pmiops.workbench.vwb.sam;

import jakarta.inject.Provider;
import org.pmiops.workbench.vwb.sam.api.GroupApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VwbSamClient {

  private final Provider<GroupApi> groupApiProvider;
  private final VwbSamRetryHandler samRetryHandler;

  private static final String GROUP_POLICY_MEMBER = "member";

  @Autowired
  public VwbSamClient(Provider<GroupApi> groupApiProvider, VwbSamRetryHandler samRetryHandler) {
    this.groupApiProvider = groupApiProvider;
    this.samRetryHandler = samRetryHandler;
  }

  /** Adds user into Sam group. */
  public void addUserToGroup(String groupName, String userEmail) {
    samRetryHandler.run(
        context -> {
          groupApiProvider.get().addEmailToGroup(groupName, GROUP_POLICY_MEMBER, userEmail, null);
          return null;
        });
  }

  /** Removes user from Sam group. */
  public void removeUserFromGroup(String groupName, String userEmail) {
    samRetryHandler.run(
        context -> {
          groupApiProvider.get().removeEmailFromGroup(groupName, GROUP_POLICY_MEMBER, userEmail);
          return null;
        });
  }
}
