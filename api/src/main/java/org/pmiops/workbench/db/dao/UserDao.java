package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<DbUser, Long> {

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

  default Map<Boolean, Long> getDisabledToCountMap() {
    return ImmutableMap.copyOf(StreamSupport.stream(findAll().spliterator(), false)
        .collect(Collectors.groupingBy(DbUser::getDisabled, Collectors.counting())));
  }
}
