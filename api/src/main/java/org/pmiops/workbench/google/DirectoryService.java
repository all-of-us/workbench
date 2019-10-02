package org.pmiops.workbench.google;

import com.google.api.services.directory.model.User;

/** Encapsulate Googe APIs for handling GSuite user accounts. */
public interface DirectoryService {
  boolean isUsernameTaken(String username);

  User getUser(String email);

  User createUser(String givenName, String familyName, String username, String contactEmail);

  User resetUserPassword(String userName);

  void deleteUser(String username);
}
