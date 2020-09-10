package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.dao.projection.PrjReportingUser;
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

  /** Find users matching the user's name or email */
  @Query(
      "SELECT dbUser FROM DbUser dbUser WHERE dbUser.dataAccessLevel IN :dals "
          + "AND ( lower(dbUser.username) LIKE lower(concat('%', :term, '%')) "
          + "OR lower(dbUser.familyName) LIKE lower(concat('%', :term, '%')) "
          + "OR lower(dbUser.givenName) LIKE lower(concat('%', :term, '%')) )")
  List<DbUser> findUsersByDataAccessLevelsAndSearchString(
      @Param("dals") List<Short> dataAccessLevels, @Param("term") String term, Sort sort);

  Set<DbUser> findByFirstRegistrationCompletionTimeNotNull();

  @Query(
      "SELECT dataAccessLevel, disabled, CASE WHEN betaAccessBypassTime IS NOT NULL THEN TRUE ELSE FALSE END AS betaIsBypassed, COUNT(userId) AS userCount "
          + "FROM DbUser "
          + "GROUP BY dataAccessLevel, disabled, CASE WHEN betaAccessBypassTime IS NOT NULL THEN TRUE ELSE FALSE END "
          + "ORDER BY NULL")
  List<UserCountGaugeLabelsAndValue> getUserCountGaugeData();

  interface UserCountGaugeLabelsAndValue {
    Short getDataAccessLevel();

    Boolean getDisabled();

    Boolean getBetaIsBypassed();

    Long getUserCount();
  }

  @Query(
      "SELECT"
          + "  u.userId,\n"
          + "  u.username,\n"
          + "  u.dataAccessLevel,\n"
          + "  u.contactEmail,\n"
          + "  u.givenName,\n"
          + "  u.familyName,\n"
          + "  u.phoneNumber,\n"
          + "  u.firstSignInTime,\n"
          + "  u.idVerificationIsValid,\n"
          + "  u.demographicSurveyCompletionTime,\n"
          + "  u.disabled,\n"
          + "  u.emailVerificationStatus,\n"
          + "  u.areaOfResearch,\n"
          + "  u.aboutYou,\n"
          + "  u.clusterCreateRetries,\n"
          + "  u.billingProjectRetries,\n"
          + "  u.betaAccessRequestTime,\n"
          + "  u.currentPosition,\n"
          + "  u.organization,\n"
          + "  u.eraCommonsLinkedNihUsername,\n"
          + "  u.eraCommonsLinkExpireTime,\n"
          + "  u.eraCommonsCompletionTime,\n"
          + "  u.dataUseAgreementCompletionTime,\n"
          + "  u.dataUseAgreementBypassTime,\n"
          + "  u.complianceTrainingCompletionTime,\n"
          + "  u.complianceTrainingBypassTime,\n"
          + "  u.betaAccessBypassTime,\n"
          + "  u.emailVerificationCompletionTime,\n"
          + "  u.emailVerificationBypassTime,\n"
          + "  u.eraCommonsBypassTime,\n"
          + "  u.idVerificationCompletionTime,\n"
          + "  u.idVerificationBypassTime,\n"
          + "  u.twoFactorAuthCompletionTime,\n"
          + "  u.twoFactorAuthBypassTime,\n"
          + "  u.complianceTrainingExpirationTime,\n"
          + "  u.dataUseAgreementSignedVersion,\n"
          + "  u.freeTierCreditsLimitDollarsOverride,\n"
          + "  u.freeTierCreditsLimitDaysOverride,\n"
          + "  u.lastFreeTierCreditsTimeCheck,\n"
          + "  u.firstRegistrationCompletionTime,\n"
          + "  u.creationTime,\n"
          + "  u.lastModifiedTime,\n"
          + "  u.professionalUrl,\n"
          + "  a.streetAddress1,\n"
          + "  a.streetAddress2,\n"
          + "  a.zipCode,\n"
          + "  a.city,\n"
          + "  a.state,\n"
          + "  a.country\n"
          + "FROM DbUser AS u\n"
          + "  LEFT OUTER JOIN DbAddress AS a ON u.userId = a.user.userId\n"
          + "  ORDER BY u.userId")
  List<PrjReportingUser> getReportingUsers();
}
