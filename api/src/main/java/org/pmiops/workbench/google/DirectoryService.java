package org.pmiops.workbench.google;

import com.google.api.services.directory.model.User;
import java.util.Map;
import java.util.Optional;

/**
 * Google APIs for handling GSuite user accounts.
 *
 * <p>Terminology used by this service:
 *
 * <p>username: The GSuite primary email address, e.g. "jdoe@researchallofus.org". This is
 * consistent with most usage in RW, where `username` refers to the full email address.
 *
 * <p>user prefix: The user-specific prefix of a GSuite email address, e.g. "jdoe".
 *
 * <p>contact email: The user's specified contact email address, which is stored in GSuite as well
 * as the RW database.
 */
public interface DirectoryService {
  /** Returns whether the given user prefix corresponds to an existing GSuite user account. */
  boolean isUsernameTaken(String userPrefix);

  /** Returns a user via username lookup. Returns null if no user was found. */
  User getUser(String username);

  Map<String, Boolean> getAllTwoFactorAuthStatuses();

  /** Looks up a user by username and returns their stored contact email address, if available. */
  Optional<String> getContactEmail(String username);

  /**
   * @param givenName
   * @param familyName
   * @param username The full RW username, e.g. "jdoe@researchallofus.org"
   * @param contactEmail The user's contact email address, e.g. "jdoe@gmail.com"
   * @return
   */
  User createUser(String givenName, String familyName, String username, String contactEmail);

  User resetUserPassword(String username);

  void deleteUser(String username);
}
