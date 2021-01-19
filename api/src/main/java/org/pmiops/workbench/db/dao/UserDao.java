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

  // Note: setter methods are included only where necessary for testing. See ProfileServiceTest.
  interface DbAdminTableUser {
    Long getUserId();

    void setUserId(Long userId);

    String getUsername();

    Short getDataAccessLevel();

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
  }

  @Query(
      "SELECT u.userId, u.username, u.dataAccessLevel, u.disabled, u.givenName, u.familyName, "
          + "u.contactEmail, i.displayName AS institutionName, "
          + "u.firstRegistrationCompletionTime, u.creationTime, u.firstSignInTime, "
          + "u.dataUseAgreementBypassTime, u.dataUseAgreementCompletionTime, "
          + "u.complianceTrainingBypassTime, u.complianceTrainingCompletionTime, "
          + "u.betaAccessBypassTime, "
          + "u.emailVerificationBypassTime, u.emailVerificationCompletionTime, "
          + "u.eraCommonsBypassTime, u.eraCommonsCompletionTime, "
          + "u.idVerificationBypassTime, u.idVerificationCompletionTime, "
          + "u.twoFactorAuthBypassTime, u.twoFactorAuthCompletionTime "
          + "FROM DbUser u "
          + "LEFT JOIN DbVerifiedInstitutionalAffiliation AS a ON u.userId = a.user.userId "
          + "LEFT JOIN DbInstitution AS i ON i.institutionId = a.institution.institutionId")
  List<DbAdminTableUser> getAdminTableUsers();
}
