package org.pmiops.workbench.absorb;

import java.util.List;

public interface AbsorbService {
  /**
   * Get details about the Registered Tier Training and the Controlled Tier Training for a user
   *
   * @param email
   * @return list of enrollments
   * @throws org.pmiops.workbench.absorb.ApiException
   */
  List<Enrollment> getActiveEnrollmentsForUser(String email) throws ApiException;
}
