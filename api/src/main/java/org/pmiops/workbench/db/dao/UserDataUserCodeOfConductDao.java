package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserDataUserCodeOfConduct;
import org.springframework.data.repository.CrudRepository;

public interface UserDataUserCodeOfConductDao extends CrudRepository<DbUserDataUserCodeOfConduct, Long> {
  List<DbUserDataUserCodeOfConduct> findByUserIdOrderByCompletionTimeDesc(long userId);
}
