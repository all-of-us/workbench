package org.pmiops.workbench.db.dao;

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

  List<DbUser> findUserByContactEmail(String contactEmail);

  @Query("SELECT user FROM DbUser user")
  List<DbUser> findUsers();

  /** Returns the user with their authorities loaded. */
  @Query("SELECT user FROM DbUser user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
  DbUser findUserWithAuthorities(@Param("id") long id);

  /** Returns the user with the page visits and authorities loaded. */
  @Query(
      "SELECT user FROM DbUser user LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits WHERE user.userId = :id")
  DbUser findUserWithAuthoritiesAndPageVisits(@Param("id") long id);

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
  List<DataAccessLevelDisabledAndBetaBypassedToCountRow>
      getDataAccessLevelDisabledAndBetaBypassedToCount();

  interface DataAccessLevelDisabledAndBetaBypassedToCountRow {
    Short getDataAccessLevel();

    Boolean getDisabled();

    Boolean getBetaIsBypassed();

    Long getUserCount();
  }
}
