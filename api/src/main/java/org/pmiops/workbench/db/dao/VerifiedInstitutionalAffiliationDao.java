package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.springframework.data.repository.CrudRepository;

public interface VerifiedInstitutionalAffiliationDao
    extends CrudRepository<DbVerifiedInstitutionalAffiliation, Long> {
  Collection<DbVerifiedInstitutionalAffiliation> findAllByInstitution(DbInstitution institution);

  Optional<DbVerifiedInstitutionalAffiliation> findFirstByUser(DbUser user);
}
