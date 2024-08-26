package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.springframework.data.repository.CrudRepository;

public interface UserInitialCreditsExpirationDao
    extends CrudRepository<DbUserInitialCreditsExpiration, Long> {}
