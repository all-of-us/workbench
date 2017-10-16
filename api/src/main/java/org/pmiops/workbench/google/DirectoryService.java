package org.pmiops.workbench.google;

/**
 * Encapsulate Googe APIs for handling GSuite user accounts.
 */
public interface DirectoryService {
  public boolean isUsernameTaken(String username);
  public void createUser(String givenName, String familyName, String username, String password);
  public void deleteUser(String username);
}
