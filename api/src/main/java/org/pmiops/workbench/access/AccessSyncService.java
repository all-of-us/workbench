package org.pmiops.workbench.access;

import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;

public interface AccessSyncService {

  /**
   * Ensures that the data access tiers for the user reflect the state of other fields on the user
   */
  DbUser updateUserAccessTiers(DbUser dbUser, Agent agent);

  /**
   * Synchronize the 2FA enablement status of the currently signed-in user between the Workbench
   * database and the gsuite directory API. This may affect the user's enabled access tiers. This
   * can only be called within the context of a user-authenticated API request.
   */
  void syncTwoFactorAuthStatus();

  /**
   * Synchronize the 2FA enablement status of the target user between the Workbench database and the
   * gsuite directory API, acting as the provided agent type. This may affect the user's enabled
   * access tiers. This can be called administratively, or from an offline cron.
   */
  DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent);

  /**
   * Synchronize the 2FA enablement status of the target user between the Workbench database and the
   * provided 2FA status, acting as the provided agent type. This may affect the user's enabled
   * access tiers. This can be called administratively, or from an offline cron.
   *
   * <p>This method is provided to allow for optimization to the lookup of the enrolled 2FA status,
   * enables batch 2FA synchronization to be implemented without repeated calls to Gsuite. The
   * source value for isEnrolledIn2FA should always be Gsuite.
   */
  DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent, boolean isEnrolledIn2FA);

  /** Syncs the current user's training status from Moodle. */
  DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  /**
   * Updates the given user's training status from Moodle.
   *
   * <p>We can fetch Moodle data for arbitrary users since we use an API key to access Moodle,
   * rather than user-specific OAuth tokens.
   *
   * <p>Using the user's email, we can get their badges from Moodle's APIs. If the badges are marked
   * valid, we store their completion dates in the database. If they are marked invalid, we clear
   * the completion dates from the database as the user will need to complete a new training.
   */
  DbUser syncComplianceTrainingStatusV2(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncDuccVersionStatus(DbUser targetUser, Agent agent);
}
