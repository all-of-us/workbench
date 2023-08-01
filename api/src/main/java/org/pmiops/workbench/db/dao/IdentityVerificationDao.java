package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbIdentityVerification;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.repository.CrudRepository;

public interface IdentityVerificationDao extends CrudRepository<DbIdentityVerification, Long> {
  Optional<DbIdentityVerification> getByUser(DbUser user);
}
