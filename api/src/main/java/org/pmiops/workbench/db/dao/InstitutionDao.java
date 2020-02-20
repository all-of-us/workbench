package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionDao extends CrudRepository<DbInstitution, Long> {
  Optional<DbInstitution> findOneByShortName(final String shortName);
}
