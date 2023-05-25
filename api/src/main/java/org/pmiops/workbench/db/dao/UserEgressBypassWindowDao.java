package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.springframework.data.repository.CrudRepository;

public interface UserEgressBypassWindowDao extends CrudRepository<DbUserEgressBypassWindow, Long> {
  // The extra "By" infix is necessary in combination with OrderBy...
  Set<DbUserEgressBypassWindow> getByUserOrderByStartTimeDesc(DbUser user);
}
