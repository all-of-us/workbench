package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.CdrVersionEntity;
import org.springframework.data.repository.CrudRepository;

public interface CdrVersionDao extends CrudRepository<CdrVersionEntity, Long> {

  CdrVersionEntity findByCdrVersionId(long id);

  CdrVersionEntity findByName(String name);

  CdrVersionEntity findByIsDefault(boolean isDefault);

  List<CdrVersionEntity> findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
      Set<Short> dataAccessLevel);
}
