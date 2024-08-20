package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.springframework.data.repository.CrudRepository;

public interface UserInitialCreditsExpirationDao
    extends CrudRepository<DbUserInitialCreditsExpiration, Long> {
  Optional<DbUserInitialCreditsExpiration> findByUser(DbUser user);
}
