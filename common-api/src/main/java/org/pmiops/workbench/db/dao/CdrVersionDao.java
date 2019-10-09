package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.data.repository.CrudRepository;

public interface CdrVersionDao extends CrudRepository<CdrVersion, Long> {

  CdrVersion findByCdrVersionId(long id);

  CdrVersion findByName(String name);

  CdrVersion findByIsDefault(boolean isDefault);

  List<CdrVersion> findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
      Set<Short> dataAccessLevel);
}
