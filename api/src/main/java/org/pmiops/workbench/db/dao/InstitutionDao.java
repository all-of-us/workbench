package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionDao extends CrudRepository<DbInstitution, Long> {
  DbInstitution findOneByApiId(final String id);
}
