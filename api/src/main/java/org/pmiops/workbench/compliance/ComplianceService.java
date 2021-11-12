package org.pmiops.workbench.compliance;

import java.util.Map;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;

public interface ComplianceService {

  enum BadgeName {
    REGISTERED_TIER_TRAINING,
    CONTROLLED_TIER_TRAINING
  }

  /**
   * Get details about the Research Ethics Training and the Data Use Agreement badges for a user
   *
   * @param email
   * @return map of badge name to badge details
   * @throws ApiException
   */
  Map<BadgeName, BadgeDetailsV2> getUserBadgesByBadgeName(String email) throws ApiException;
}
