package org.pmiops.workbench.google;

import com.google.api.services.directory.model.User;

/** Encapsulate Googe APIs for handling GSuite user accounts. */
public interface DirectoryService {
  boolean isUsernameTaken(String username);

  /**
   * Returns a user via email address lookup.
   */
  User getUser(String email);

  /**
   * Returns a user via username lookup (e.g. the user's GSuite email address without the domain suffix.
   */
  User getUserByUsername(String username);

  /**
   * Looks up a user by username and returns their stored contact email address. If no contact
   * email is stored in G Suite, then null is returned.
   */
  String getContactEmailAddress(String username);

  User createUser(String givenName, String familyName, String username, String contactEmail);

  User resetUserPassword(String userName);

  void deleteUser(String username);
}
