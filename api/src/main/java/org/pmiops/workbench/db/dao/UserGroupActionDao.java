package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUserGroupAction;
import org.springframework.data.repository.CrudRepository;

public interface UserGroupActionDao extends CrudRepository<DbUserGroupAction, Long> {
}
