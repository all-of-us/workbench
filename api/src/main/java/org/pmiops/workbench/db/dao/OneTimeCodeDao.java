package org.pmiops.workbench.db.dao;

import java.util.UUID;
import org.pmiops.workbench.db.model.DbOneTimeCode;
import org.springframework.data.repository.CrudRepository;

public interface OneTimeCodeDao extends CrudRepository<DbOneTimeCode, UUID> {}
