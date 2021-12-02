package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.data.repository.CrudRepository;

public interface CdrVersionDao extends CrudRepository<DbCdrVersion, Long> {
  DbCdrVersion findByName(String name);

  List<DbCdrVersion> findByAccessTierOrderByCreationTimeDesc(DbAccessTier accessTier);
}
