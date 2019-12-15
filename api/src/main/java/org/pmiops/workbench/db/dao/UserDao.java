package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<DbUser, Long> {

  DbUser findUserByUserName(String userName);

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
      "SELECT user FROM DbUser user WHERE user.dataAccessLevel IN :dals AND ( lower(user.userName) LIKE lower(concat('%', :term, '%')) OR lower(user.familyName) LIKE lower(concat('%', :term, '%')) OR lower(user.givenName) LIKE lower(concat('%', :term, '%')) )")
  List<DbUser> findUsersByDataAccessLevelsAndSearchString(
      @Param("dals") List<Short> dataAccessLevels, @Param("term") String term, Sort sort);
}
