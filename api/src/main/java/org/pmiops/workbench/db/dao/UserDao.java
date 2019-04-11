package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;

import org.pmiops.workbench.db.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<User, Long> {

  User findUserByEmail(String email);
  User findUserByUserId(long userId);

  List<User> findUserByContactEmail(String contactEmail);


  @Query("SELECT user FROM User user")
  List<User> findUsers();

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
  @Query("SELECT user FROM User user LEFT JOIN FETCH user.authorities LEFT JOIN FETCH user.pageVisits WHERE user.userId = :id")
  User findUserWithAuthoritiesAndPageVisits(@Param("id") long id);

  /**
   * Find users matching the user's name or email
   */
  @Query("SELECT user FROM User user WHERE user.dataAccessLevel IN :dals AND ( lower(user.email) LIKE lower(concat('%', :term, '%')) OR lower(user.familyName) LIKE lower(concat('%', :term, '%')) OR lower(user.givenName) LIKE lower(concat('%', :term, '%')) )")
  List<User> findUsersByDataAccessLevelsAndSearchString(@Param("dals") List<Short> dataAccessLevels, @Param("term") String term, Sort sort);

}
