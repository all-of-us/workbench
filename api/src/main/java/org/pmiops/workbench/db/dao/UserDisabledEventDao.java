package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbUserDisabledEvent;
import org.springframework.data.repository.CrudRepository;

public interface UserDisabledEventDao extends CrudRepository<DbUserDisabledEvent, Long> {
  // The extra "By" infix is necessary in combination with OrderBy...
  Set<DbUserDisabledEvent> getByUserIdOrderByUpdateTimeDesc(Long userId);
}
