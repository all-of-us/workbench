package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserGroup;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionUserGroupDao extends CrudRepository<DbInstitutionUserGroup, Long> {

  Set<DbInstitutionUserGroup> getByInstitution(final DbInstitution institution);

  long deleteByInstitution(final DbInstitution institution);
}
