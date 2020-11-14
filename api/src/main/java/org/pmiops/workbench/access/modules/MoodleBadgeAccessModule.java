package org.pmiops.workbench.access.modules;

import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.MoodleBadge;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;

/** Intermediate class for working with with Moodle-backed access module requirements. */
public abstract class MoodleBadgeAccessModule implements AccessModuleService {
  private static final Logger log = Logger.getLogger(MoodleBadgeAccessModule.class.getName());

  private ComplianceService complianceService;

  public MoodleBadgeAccessModule(ComplianceService complianceService) {
    this.complianceService = complianceService;
  }

  /**
   * Use the Moodle API to establish whether the user has access to the correct module
   *
   * @param user
   * @return
   */
  @Override
  public AccessScore scoreUser(DbUser user) {
    try {
      final Optional<BadgeDetailsV2> details =
          complianceService.getUserBadgeDetails(user.getUsername(), getBadge());
      return details.map(this::badgeDetailsToAccessScore).orElse(AccessScore.NOT_ATTEMPTED);
    } catch (ApiException e) {
      log.warning(e.getMessage());
      return AccessScore.PENDING;
    }
  }

  /**
   * A Moodle "Badge" corresponds to one trraining course, which maps to an individual AccessModule
   * in AoU. Implementers need to return the badge name
   *
   * @return
   */
  public abstract MoodleBadge getBadge();

  private AccessScore badgeDetailsToAccessScore(BadgeDetailsV2 details) {
    if (details.getValid()) {
      return AccessScore.PASSED;
    } else {
      return AccessScore.FAILED;
    }
  }
}
