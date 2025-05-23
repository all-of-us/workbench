package org.pmiops.workbench.db.dao;

import com.google.api.services.oauth2.model.Userinfo;
import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.GeneralDiscoverySource;
import org.pmiops.workbench.model.PartnerDiscoverySource;
import org.springframework.data.domain.Sort;

public interface UserService {
  /**
   * Updates a user record with a modifier function.
   *
   * <p>Ensures that the data access tiers for the user reflect the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent);

  DbUser createServiceAccountUser(String email);

  // version used by DevUserRegistrationService
  DbUser createUser(
      Userinfo oAuth2Userinfo,
      String contactEmail,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String areaOfResearch,
      String professionalUrl,
      List<GeneralDiscoverySource> generalDiscoverySources,
      String generalDiscoverySourceOtherText,
      List<PartnerDiscoverySource> partnerDiscoverySources,
      String partnerDiscoverySourceOtherText,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      DbDemographicSurveyV2 dbDemographicSurveyv2,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation);

  // TODO(jaycarlton): Move compliance-related methods to a new UserComplianceService or similar
  DbUser submitDUCC(DbUser user, Integer duccSignedVersion, String initials);

  void validateAllOfUsTermsOfService(Integer tosVersion);

  /**
   * Is the user up-to-date with our Terms of Service?
   *
   * @param dbUser the current user - used only to generate error messages
   * @return true only if the user has accepted the latest version of the AoU Terms of Service
   */
  boolean hasSignedLatestAoUTermsOfService(@Nonnull DbUser dbUser);

  /**
   * Is the user up-to-date with both AoU and Terra Terms of Service?
   *
   * @param dbUser the current user - used only to generate error messages
   * @return true only if the user has accepted the latest version of both AoU and Terra Terms of
   *     Service
   */
  boolean hasSignedLatestTermsOfServiceForBoth(@Nonnull DbUser dbUser);

  // Registers that a user has agreed to a given version of the AoU Terms of Service.
  void submitAouTermsOfService(@Nonnull DbUser dbUser, @Nonnull Integer tosVersion);

  // Registers that a user has accepted the latest version of the Terra Terms of Service.
  @Deprecated // to be replaced as part of RW-11416
  void acceptTerraTermsOfServiceDeprecated(@Nonnull DbUser dbUser);

  DbUser setDisabledStatus(Long userId, boolean disabled);

  List<Long> getAllUserIds();

  List<DbUser> getAllUsers();

  List<Long> getAllUserIdsWithCurrentTierAccess();

  List<Long> getAllUserIdsWithActiveInitialCredits();

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

  /** Loads all users given list of usernames. */
  List<DbUser> findUsersByUsernames(List<String> usernames);

  /**
   * Loads only active users given list of usernames.
   *
   * @param usernames usernames to search for
   * @return a set of DbUser objects that are active only.
   */
  Set<DbUser> findActiveUsersByUsernames(List<String> usernames);

  List<DbUser> findUsersById(List<Long> ids);

  DbUser syncEraCommonsStatus();

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

  DbUser syncDuccVersionStatus(DbUser targetUser, Agent agent);

  Optional<DbUser> getByUsername(String username);

  // same as the above, but throw NotFoundException if not found
  DbUser getByUsernameOrThrow(String username);

  Map<String, DbUser> getUsersMappedByUsernames(Collection<String> usernames);

  Optional<DbUser> getByDatabaseId(long databaseId);

  boolean hasAuthority(long userId, Authority required);

  Optional<DbUser> findUserWithAuthoritiesAndPageVisits(long userId);

  DbUser updateIdentityStatus(String userName);

  DbUser updateRasLinkLoginGovStatus(String loginGovUserName);

  /** Link eRA commons account in RW using RAS as source of truth. */
  DbUser updateRasLinkEraStatus(String eRACommonsUsername);

  /** Confirm that a user's profile is up to date, for annual renewal compliance purposes. */
  DbUser confirmProfile(DbUser u);

  /** Confirm that a user has either reported any AoU-related publications, or has none. */
  DbUser confirmPublications();

  /** Send an Access Renewal Expiration or Warning email to the user, if appropriate */
  void maybeSendAccessTierExpirationEmails(DbUser user);

  /** Signs a user out of all web and device sessions and reset their sign-in cookies. */
  void signOut(DbUser user);

  boolean isServiceAccount(DbUser user);

  boolean hasExistingAccount(String contactEmail);
}
