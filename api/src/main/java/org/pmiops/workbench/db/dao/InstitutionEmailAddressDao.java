package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionEmailAddressDao
    extends CrudRepository<DbInstitutionEmailAddress, Long> {

  Set<DbInstitutionEmailAddress> getByInstitution(final DbInstitution institution);

  long deleteByInstitution(final DbInstitution institution);
}
