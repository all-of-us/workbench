package org.pmiops.workbench.privateWorkbench;


import org.pmiops.workbench.privateWorkbench.model.Profile;

public interface PrivateWorkbenchService {

  /**
   * @return the Workbench profile for the requesting user.
   */
  Profile getMe() throws ApiException;
}
