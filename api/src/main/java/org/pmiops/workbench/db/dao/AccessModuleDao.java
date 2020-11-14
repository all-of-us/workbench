package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.springframework.data.repository.CrudRepository;

public interface AccessModuleDao extends CrudRepository<DbAccessModule, Long> {
  Optional<DbAccessModule> findByAccessModuleId(long id);
}
