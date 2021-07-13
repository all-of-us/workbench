package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.springframework.data.repository.CrudRepository;

public interface UserAccessModuleDao extends CrudRepository<DbUserAccessModule, Long> {
  Optional<DbUserAccessModule> getByUserAndAccessModule(DbUser user, DbAccessModule accessModule);

  List<DbUserAccessModule> getAllByUser(DbUser user);
}
