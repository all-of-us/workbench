package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
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
      "SELECT\n"
          + "  u.aboutYou,\n"
          + "  u.areaOfResearch,\n"
          + "  u.complianceTrainingBypassTime,\n"
          + "  u.complianceTrainingCompletionTime,\n"
          + "  u.complianceTrainingExpirationTime,\n"
          + "  u.contactEmail,\n"
          + "  u.creationTime,\n"
          + "  u.currentPosition,\n"
          + "  u.dataAccessLevel,\n"
          + "  u.dataUseAgreementBypassTime,\n"
          + "  u.dataUseAgreementCompletionTime,\n"
          + "  u.dataUseAgreementSignedVersion,\n"
          + "  u.demographicSurveyCompletionTime,\n"
          + "  u.disabled,\n"
          + "  u.eraCommonsBypassTime,\n"
          + "  u.eraCommonsCompletionTime,\n"
          + "  u.familyName,\n"
          + "  u.firstRegistrationCompletionTime,\n"
          + "  u.firstSignInTime,\n"
          + "  u.freeTierCreditsLimitDaysOverride,\n"
          + "  u.freeTierCreditsLimitDollarsOverride,\n"
          + "  u.givenName,\n"
          + "  u.lastModifiedTime,\n"
          + "  u.professionalUrl,\n"
          + "  u.twoFactorAuthBypassTime,\n"
          + "  u.twoFactorAuthCompletionTime,\n"
          + "  u.userId,\n"
          + "  u.username,\n"
          + "  a.city,\n"
          + "  a.country,\n"
          + "  a.state,\n"
          + "  a.streetAddress1,\n"
          + "  a.streetAddress2,\n"
          + "  a.zipCode\n"
          + "FROM DbUser u\n"
          + "  LEFT OUTER JOIN DbAddress AS a ON u.userId = a.user.userId\n"
          + "  ORDER BY u.userId")
  List<ProjectedReportingUser> getReportingUsers();
}
