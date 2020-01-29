package org.pmiops.workbench.compliance;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV1;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;

public interface ComplianceService {

  /**
   * Get Moodle Id associated with Aou User email id
   *
   * @param email
   * @return Moodle Id
   * @throws ApiException
   */
  Integer getMoodleId(String email) throws ApiException;

  /**
   * Get the list of badges earned by User
   *
   * @param userMoodleId
   * @return list of badges/completed training by user
   * @throws ApiException
   */
  @Deprecated
  List<BadgeDetailsV1> getUserBadgeV1(int userMoodleId) throws ApiException;

  /**
   * Get details about the Research Ethics Training and the Data Use Agreement badges for a user
   *
   * @param email
   * @return map of badge name to badge details
   * @throws ApiException
   */
  Map<String, BadgeDetailsV2> getUserBadgesByBadgeName(String email) throws ApiException;

  String getResearchEthicsTrainingField();
}
