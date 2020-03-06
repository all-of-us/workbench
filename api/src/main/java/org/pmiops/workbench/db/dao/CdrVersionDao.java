package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.springframework.data.repository.CrudRepository;

public interface CdrVersionDao extends CrudRepository<DbCdrVersion, Long> {

  DbCdrVersion findByCdrVersionId(long id);

  DbCdrVersion findByName(String name);

  DbCdrVersion findByIsDefault(boolean isDefault);

  List<DbCdrVersion> findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
      Set<Short> dataAccessLevel);
}
