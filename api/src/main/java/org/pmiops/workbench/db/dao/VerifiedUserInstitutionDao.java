package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedUserInstitution;
import org.springframework.data.repository.CrudRepository;

public interface VerifiedUserInstitutionDao
    extends CrudRepository<DbVerifiedUserInstitution, Long> {
  Collection<DbVerifiedUserInstitution> findAllByInstitution(DbInstitution institution);

  Optional<DbVerifiedUserInstitution> findFirstByUser(DbUser user);
}
