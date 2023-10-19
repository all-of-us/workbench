package org.pmiops.workbench.absorb;

import java.util.List;

public interface AbsorbService {
  Credentials fetchCredentials(String email) throws ApiException;

  Boolean userHasLoggedIntoAbsorb(Credentials credentials);

  /**
   * Get details about the Registered Tier Training and the Controlled Tier Training for a user
   *
   * @return list of enrollments
   */
  List<Enrollment> getActiveEnrollmentsForUser(Credentials credentials) throws ApiException;
}
