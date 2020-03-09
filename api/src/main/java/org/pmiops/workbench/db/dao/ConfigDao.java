package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbConfig;
import org.springframework.data.repository.CrudRepository;

public interface ConfigDao extends CrudRepository<DbConfig, String> {}
