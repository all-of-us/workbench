package org.pmiops.workbench.google;

import com.google.api.services.directory.model.User;
import java.util.Optional;

/** Encapsulate Googe APIs for handling GSuite user accounts. */
public interface DirectoryService {
  boolean isUsernameTaken(String username);

  /** Returns a user via email address lookup. Returns null if no user was found. */
  User getUser(String email);

  /**
   * Returns a user via username lookup (e.g. the user's GSuite email address without the domain
   * suffix. Returns null if no user was found.
   */
  User getUserByUsername(String username);

  /** Looks up a user by username and returns their stored contact email address, if available. */
  Optional<String> getContactEmail(String username);

  /**
   * Looks up a user by GSuite email address and returns their stored contact email address, if
   * available.
   */
  Optional<String> getContactEmailFromGSuiteEmail(String gSuiteEmailAddress);

  User createUser(String givenName, String familyName, String username, String contactEmail);

  User resetUserPassword(String userName);

  void deleteUser(String username);
}
