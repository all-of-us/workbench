package org.pmiops.workbench.privateworkbench;


import org.pmiops.workbench.privateworkbench.model.Profile;

public interface PrivateWorkbenchService {

  /**
   * @return the Workbench profile for the requesting user.
   */
  Profile getMe() throws ApiException;
}
