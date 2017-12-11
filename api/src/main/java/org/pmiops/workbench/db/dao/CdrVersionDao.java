package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.data.repository.CrudRepository;

public interface CdrVersionDao extends CrudRepository<CdrVersion, Long> {

  CdrVersion findByName(String name);
}
