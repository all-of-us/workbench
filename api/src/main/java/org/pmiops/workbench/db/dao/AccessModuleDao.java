package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.springframework.data.repository.CrudRepository;

public interface AccessModuleDao extends CrudRepository<DbAccessModule, Long> {
  List<DbAccessModule> findAll();

  Optional<DbAccessModule> findOneByShortName(AccessModuleName name);
}
