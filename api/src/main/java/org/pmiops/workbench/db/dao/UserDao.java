package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;

import org.pmiops.workbench.db.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<User, Long> {

  User findUserByEmail(String email);
  User findUserByUserId(long userId);

  List<User> findUserByContactEmail(String contactEmail);

  /**
   * Returns the users who's identities have not been validated
   */
  @Query("SELECT user FROM User user WHERE user.idVerificationIsValid IS NULL OR user.idVerificationIsValid = false")
  List<User> findUserNotValidated();

  /**
   * Returns the user with their authorities loaded.
   */
  @Query("SELECT user FROM User user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
  User findUserWithAuthorities(@Param("id") long id);

  @Query("SELECT DISTINCT user.freeTierBillingProjectName FROM User user\n" +
      "WHERE user.freeTierBillingProjectName IS NOT NULL")
  Set<String> getAllUserProjects();

  /**
   * Returns the user with the page visits and authorities loaded.
   */

  @Query("Select user FROM User user LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits WHERE user.userId = :id")
  User findUserWithAuthoritiesAndPageVisits(@Param("id") long id);
}
