package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  List<DbUser> findUsersByUsernameIn(Collection<String> username);

  default Map<String, DbUser> getUsersMappedByUsernames(Collection<String> usernames) {
    return findUsersByUsernameIn(usernames).stream()
        .collect(Collectors.toMap(DbUser::getUsername, Function.identity()));
  }

  DbUser findUserByUserId(long userId);

  List<DbUser> findUsersByContactEmail(String contactEmail);

  @Query(
      value =
          "select u.* "
              + "from user u "
              + "join user_verified_institutional_affiliation uvia on (u.user_id = uvia.user_id) "
              + "join institution i on (uvia.institution_id = i.institution_id) "
              + "where i.short_name = 'AouOps' "
              + "and u.contact_email = :contactEmail",
      nativeQuery = true)
  List<DbUser> findOpsUsersByContactEmail(@Param("contactEmail") String contactEmail);

  List<DbUser> findUsersByUserIdIn(List<Long> userIds);

  @Query("SELECT user.id FROM DbUser user")
  List<Long> findUserIds();

  @Query("SELECT user FROM DbUser user")
  List<DbUser> findUsers();

  @Query(
      "SELECT user.userId FROM DbUser user "
          + "JOIN DbUserInitialCreditsExpiration exp ON exp.user.userId = user.userId "
          + "WHERE exp.expirationCleanupTime IS NULL")
  List<Long> findUserIdsWithActiveInitialCredits();

  /** Returns the user with their authorities loaded. */
  @Query("SELECT user FROM DbUser user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
  DbUser findUserWithAuthorities(@Param("id") long id);

  /** Returns the user with the page visits and authorities loaded. */
  @Query(
      "SELECT user FROM DbUser user "
          + "LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits "
          + "WHERE user.userId = :id")
  DbUser findUserWithAuthoritiesAndPageVisits(@Param("id") long id);

  // Find users matching the requested access tier and a (name or username) search term.
  @Query(
      "SELECT dbUser FROM DbUser dbUser "
          + "JOIN DbUserAccessTier uat ON uat.user.userId = dbUser.userId "
          + "JOIN DbAccessTier tier ON uat.accessTier.accessTierId = tier.accessTierId "
          + "WHERE tier.shortName = :shortName "
          + "  AND uat.tierAccessStatus = 1 " // TierAccessStatus.ENABLED
          + "  AND (lower(dbUser.username) LIKE lower(concat('%', :term, '%')) "
          + "    OR lower(dbUser.familyName) LIKE lower(concat('%', :term, '%')) "
          + "    OR lower(dbUser.givenName) LIKE lower(concat('%', :term, '%')))")
  List<DbUser> findUsersBySearchStringAndTier(
      @Param("term") String term, Sort sort, @Param("shortName") String accessTierShortName);

  Set<DbUser> findUsersByUsernameInAndDisabledFalse(List<String> usernames);

  @Query(
      "SELECT DISTINCT dbUser.userId FROM DbUser dbUser "
          + "JOIN DbUserAccessTier uat ON uat.user.userId = dbUser.userId "
          + "WHERE uat.tierAccessStatus = 1 ") // TierAccessStatus.ENABLED
  List<Long> findUserIdsWithCurrentTierAccess();

  @Query(
      "SELECT u FROM DbUser u "
          + "LEFT JOIN FETCH u.newUserSatisfactionSurveyOneTimeCode otc "
          + "LEFT JOIN FETCH u.newUserSatisfactionSurvey nuss "
          + "WHERE u.creationTime BETWEEN :minCreationTime AND :maxCreationTime "
          + "  AND otc.id IS NULL "
          + "  AND nuss.id IS NULL")
  List<DbUser> findUsersBetweenCreationTimeWithoutNewUserSurveyOrCode(
      @Param("minCreationTime") Timestamp minCreationTime,
      @Param("maxCreationTime") Timestamp maxCreationTime);

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

    String getInstitutionShortName();

    void setInstitutionName(String institutionName);

    Timestamp getFirstSignInTime();

    Timestamp getCreationTime();

    Timestamp getDuccBypassTime();

    Timestamp getComplianceTrainingBypassTime();

    Timestamp getCtComplianceTrainingBypassTime();

    Timestamp getEraCommonsBypassTime();

    Timestamp getTwoFactorAuthBypassTime();

    Timestamp getProfileConfirmationBypassTime();

    Timestamp getPublicationConfirmationBypassTime();

    Timestamp getIdentityBypassTime();

    String getAccessTierShortNames();
  }

  /**
   * Important! Make sure add alias and it matches name in {@link DbAdminTableUser}, otherwise JPA
   * will return null if the query column name does not match the variable name in interface
   * projection.
   */
  @Query(
      // JPQL doesn't allow join on subquery
      nativeQuery = true,
      value =
          "SELECT u.user_id AS userId, "
              + "u.email AS username, "
              + "u.disabled AS disabled, "
              + "u.given_name AS givenName, "
              + "u.family_name AS familyName, "
              + "u.contact_email AS contactEmail, "
              + "i.display_name AS institutionName, "
              + "i.short_name AS institutionShortName, "
              + "u.creation_time AS creationTime, "
              + "u.first_sign_in_time AS firstSignInTime, "
              + "uamd.ducc_bypass_time AS duccBypassTime, "
              + "uamrt.compliance_training_bypass_time AS complianceTrainingBypassTime, "
              + "uamct.ct_compliance_training_bypass_time AS ctComplianceTrainingBypassTime, "
              + "uame.era_commons_bypass_time AS eraCommonsBypassTime, "
              + "uamprofile.profile_confirmation_bypass_time AS profileConfirmationBypassTime, "
              + "uampublication.publication_confirmation_bypass_time AS publicationConfirmationBypassTime, "
              + "uamt.two_factor_auth_bypass_time AS twoFactorAuthBypassTime, "
              + "uami.identity_bypass_time AS identityBypassTime, "
              + "t.access_tier_short_names AS accessTierShortNames "
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
              + ") as t ON t.user_id = u.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS era_commons_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'ERA_COMMONS' "
              + ") as uame ON u.user_id = uame.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS two_factor_auth_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'TWO_FACTOR_AUTH' "
              + ") as uamt ON u.user_id = uamt.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS profile_confirmation_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'PROFILE_CONFIRMATION' "
              + ") as uamprofile ON u.user_id = uamprofile.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS publication_confirmation_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'PUBLICATION_CONFIRMATION' "
              + ") as uampublication ON u.user_id = uampublication.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS compliance_training_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'RT_COMPLIANCE_TRAINING' "
              + ") as uamrt ON u.user_id = uamrt.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS ct_compliance_training_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'CT_COMPLIANCE_TRAINING' "
              + ") as uamct ON u.user_id = uamct.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS ducc_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'DATA_USER_CODE_OF_CONDUCT' "
              + ") as uamd ON u.user_id = uamd.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS ras_link_login_gov_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'RAS_LOGIN_GOV' "
              + ") as uamr ON u.user_id = uamr.user_id "
              + "LEFT JOIN ( "
              + "  SELECT uam.user_id, "
              + "    uam.bypass_time AS identity_bypass_time "
              + "  FROM user_access_module uam "
              + "  JOIN access_module am ON am.access_module_id=uam.access_module_id "
              + "  WHERE am.name = 'IDENTITY' "
              + ") as uami ON u.user_id = uami.user_id ")
  List<DbAdminTableUser> getAdminTableUsers();
}
