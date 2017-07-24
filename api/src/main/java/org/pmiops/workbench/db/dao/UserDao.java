package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserDao extends CrudRepository<User, Long> {

  User findUserByEmail(String email);
}
