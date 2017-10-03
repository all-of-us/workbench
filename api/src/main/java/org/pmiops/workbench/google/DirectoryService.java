package org.pmiops.workbench.google;

import org.pmiops.workbench.auth.Public;

/**
 * Encapsulate Googe APIs for handling GSuite user accounts.
 */
public interface DirectoryService {
  @Public public boolean isUsernameTaken(String username);
}
