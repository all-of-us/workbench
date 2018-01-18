package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserDao extends CrudRepository<User, Long> {

  User findUserByEmail(String email);
  List<User> findByBlockscoreVerificationIsValidIsNotNull();

  /**
   * Returns the user with their authorities loaded.
   */
  @Query("SELECT user FROM User user LEFT JOIN FETCH user.authorities WHERE user.userId = :id")
  User findUserWithAuthorities(@Param("id") long id);
}
