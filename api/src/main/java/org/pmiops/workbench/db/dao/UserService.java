package org.pmiops.workbench.db.dao;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.springframework.data.domain.Sort;

public interface UserService {
  DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent);

  DbUser createServiceAccountUser(String email);

  // version used by DevUserRegistrationService
  DbUser createUser(
      Userinfoplus oAuth2Userinfo,
      String contactEmail,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      String professionalUrl,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser updateUserWithConflictHandling(DbUser user);

  // TODO(jaycarlton): Move compliance-related methods to a new UserComplianceService or similar
  DbUser submitDataUseAgreement(
      DbUser user, Integer dataUseAgreementSignedVersion, String initials);

  // Registers that a user has agreed to a given version of the Terms of Service.
  void submitTermsOfService(DbUser dbUser, Integer tosVersion);

  void setDataUseAgreementNameOutOfDate(String newGivenName, String newFamilyName);

  void setDataUseAgreementBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setComplianceTrainingBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setBetaAccessBypassTime(Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setEraCommonsBypassTime(Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setTwoFactorAuthBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  void setRasLinkLoginGovBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime);

  DbUser setDisabledStatus(Long userId, boolean disabled);

  List<DbUser> getAllUsers();

  List<DbUser> getAllUsersExcludingDisabled();

  @Deprecated // use or create an auditor in org.pmiops.workbench.actionaudit.auditors
  void logAdminUserAction(long targetUserId, String targetAction, Object oldValue, Object newValue);

  @Deprecated // use or create an auditor in org.pmiops.workbench.actionaudit.auditors
  void logAdminWorkspaceAction(
      long targetWorkspaceId, String targetAction, Object oldValue, Object newValue);

  /**
   * Find users with Registered Tier access whose name or username match the supplied search terms.
   *
   * @param term User-supplied search term
   * @param sort Option(s) for ordering query results
   * @return the List of DbUsers which meet the search and access requirements
   * @deprecated use {@link #findUsersBySearchString(String, Sort, String)} instead.
   */
  @Deprecated
  List<DbUser> findUsersBySearchString(String term, Sort sort);

  /**
   * Find users whose name or username match the supplied search terms and who have the appropriate
   * access tier.
   *
   * @param term User-supplied search term
   * @param sort Option(s) for ordering query results
   * @param accessTierShortName the shortName of the access tier to check
   * @return the List of DbUsers which meet the search and access requirements
   */
  List<DbUser> findUsersBySearchString(String term, Sort sort, String accessTierShortName);

  DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncComplianceTrainingStatusV2(DbUser user, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException;

  DbUser syncEraCommonsStatus();

  DbUser syncEraCommonsStatusUsingImpersonation(DbUser user, Agent agent)
      throws IOException, org.pmiops.workbench.firecloud.ApiException;

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

  int getCurrentDuccVersion();

  Optional<DbUser> getByUsername(String username);

  // same as the above, but throw NotFoundException if not found
  DbUser getByUsernameOrThrow(String username);

  Optional<DbUser> getByDatabaseId(long databaseId);

  void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest);

  boolean hasAuthority(long userId, Authority required);

  Optional<DbUser> findUserWithAuthoritiesAndPageVisits(long userId);

  DbUser updateRasLinkLoginGovStatus(String loginGovUserName);

  /** Confirm that a user's profile is up to date, for annual renewal compliance purposes. */
  DbUser confirmProfile();

  /** Confirm that a user has either reported any AoU-related publications, or has none. */
  DbUser confirmPublications();
}
