package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbAccessTier;
import org.springframework.data.repository.CrudRepository;

public interface AccessTierDao extends CrudRepository<DbAccessTier, Long> {
  DbAccessTier findOneByShortName(String shortName);
}
