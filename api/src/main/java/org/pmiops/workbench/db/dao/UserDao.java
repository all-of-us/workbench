package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<DbUser, Long> {

  /**
   * Looks up a user by their "username", which is the full G Suite email address of the user (e.g.
   * "john.doe@researchallofus.org").
   *
   * @param username
   * @return
   */
  DbUser findUserByUsername(String username);

  DbUser findUserByUserId(long userId);

  @Query("SELECT user FROM DbUser user")
  List<DbUser> findUsers();

  @Query("SELECT user FROM DbUser user WHERE user.disabled = FALSE")
  List<DbUser> findUsersExcludingDisabled();

  /** Returns the user with their authorities loaded. */
  @Query("SELECT user FROM DbUser user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
  DbUser findUserWithAuthorities(@Param("id") long id);

  /** Returns the user with the page visits and authorities loaded. */
  @Query(
      "SELECT user FROM DbUser user LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits WHERE user.userId = :id")
  DbUser findUserWithAuthoritiesAndPageVisits(@Param("id") long id);

  /** Returns the user with the page visits and authorities loaded. */
  @Query(
      "SELECT user"
          + " FROM DbUser user"
          + " LEFT JOIN FETCH user.authorities"
          + " LEFT JOIN FETCH user.pageVisits"
          + " ORDER BY NULL")
  Set<DbUser> findAllUsersWithAuthoritiesAndPageVisits();

  // Find users matching the requested access tier and a (name or username) search term.
  @Query(
      "SELECT dbUser FROM DbUser dbUser "
          + "JOIN DbUserAccessTier uat ON uat.user.userId = dbUser.userId "
          + "JOIN DbAccessTier tier ON uat.accessTier.accessTierId = tier.accessTierId "
          + "WHERE tier.shortName = :shortName AND "
          + "  (lower(dbUser.username) LIKE lower(concat('%', :term, '%')) "
          + "  OR lower(dbUser.familyName) LIKE lower(concat('%', :term, '%')) "
          + "  OR lower(dbUser.givenName) LIKE lower(concat('%', :term, '%')))")
  List<DbUser> findUsersBySearchStringAndTier(
      @Param("term") String term, Sort sort, @Param("shortName") String accessTierShortName);

  interface UserCountByDisabledAndAccessTiers {
    Long getUserCount();

    Boolean getDisabled();

    String getAccessTierShortNames();
  }

  @Query(
      // JPQL doesn't allow join on subquery
      nativeQuery = true,
      value =
          "SELECT COUNT(u.user_id) AS userCount, u.disabled AS disabled, "
              + "COALESCE(t.access_tier_short_names, '[unregistered]') AS accessTierShortNames "
              + "FROM user u "
              + "LEFT JOIN ("
              + "  SELECT u.user_id, "
              + "  GROUP_CONCAT(DISTINCT a.short_name ORDER BY a.short_name) AS accessTierShortNames "
              + "  FROM user u "
              + "  JOIN user_access_tier uat ON u.user_id = uat.user_id "
              + "  JOIN access_tier a ON a.access_tier_id = uat.access_tier_id "
              + "  WHERE uat.access_status = 1 " // ENABLED
              + "  GROUP BY u.user_id"
              + ") as t ON t.user_id = u.user_id "
              + "GROUP BY disabled, accessTierShortNames ")
  List<UserCountByDisabledAndAccessTiers> getUserCountGaugeData();

  // Note: setter methods are included only where necessary for testing. See ProfileServiceTest.
  interface DbAdminTableUser {
    Long getUserId();

    void setUserId(Long userId);

    String getUsername();

    Boolean getDisabled();

    void setDisabled(Boolean disabled);

    String getGivenName();

    String getFamilyName();

    String getContactEmail();

    void setContactEmail(String contactEmail);

    String getInstitutionName();

    void setInstitutionName(String institutionName);

    Timestamp getFirstRegistrationCompletionTime();

    Timestamp getFirstSignInTime();

    Timestamp getCreationTime();

    Timestamp getDataUseAgreementBypassTime();

    Timestamp getDataUseAgreementCompletionTime();

    Timestamp getComplianceTrainingBypassTime();

    Timestamp getComplianceTrainingCompletionTime();

    Timestamp getBetaAccessBypassTime();

    Timestamp getEmailVerificationBypassTime();

    Timestamp getEmailVerificationCompletionTime();

    Timestamp getEraCommonsBypassTime();

    Timestamp getEraCommonsCompletionTime();

    Timestamp getIdVerificationBypassTime();

    Timestamp getIdVerificationCompletionTime();

    Timestamp getTwoFactorAuthBypassTime();

    Timestamp getTwoFactorAuthCompletionTime();

    Timestamp getRasLinkLoginGovBypassTime();

    Timestamp getRasLinkLoginGovCompletionTime();

    String getAccessTierShortNames();
  }

  @Query(
      // JPQL doesn't allow join on subquery
      nativeQuery = true,
      value =
          "SELECT u.user_id, u.email AS username, u.disabled, u.given_name, u.family_name, "
              + "u.contact_email, i.display_name AS institution_name, "
              + "u.first_registration_completion_time, u.creation_time, u.first_sign_in_time, "
              + "u.data_use_agreement_bypass_time, u.data_use_agreement_completion_time, "
              + "u.compliance_training_bypass_time, u.compliance_training_completion_time, "
              + "u.beta_access_bypass_time, "
              + "u.email_verification_bypass_time, u.email_verification_completion_time, "
              + "u.era_commons_bypass_time, u.era_commons_completion_time, "
              + "u.id_verification_bypass_time, u.id_verification_completion_time, "
              + "u.two_factor_auth_bypass_time, u.two_factor_auth_completion_time, "
              + "u.ras_link_login_gov_bypass_time, u.ras_link_login_gov_completion_time, "
              + "t.access_tier_short_names "
              + "FROM user u "
              + "LEFT JOIN user_verified_institutional_affiliation AS uvia ON u.user_id = uvia.user_id "
              + "LEFT JOIN institution AS i ON i.institution_id = uvia.institution_id "
              + "LEFT JOIN ("
              + "  SELECT u.user_id, "
              + "  GROUP_CONCAT(DISTINCT a.short_name ORDER BY a.short_name) AS access_tier_short_names "
              + "  FROM user u "
              + "  JOIN user_access_tier uat ON u.user_id = uat.user_id "
              + "  JOIN access_tier a ON a.access_tier_id = uat.access_tier_id "
              + "  WHERE uat.access_status = 1 " // ENABLED
              + "  GROUP BY u.user_id"
              + ") as t ON t.user_id = u.user_id")
  List<DbAdminTableUser> getAdminTableUsers();
}
