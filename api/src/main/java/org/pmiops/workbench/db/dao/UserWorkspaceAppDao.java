package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbUserWorkspaceApp;
import org.springframework.data.repository.CrudRepository;

public interface UserWorkspaceAppDao extends CrudRepository<DbUserWorkspaceApp, Long> {

  Optional<DbUserWorkspaceApp> findDbUserWorkspaceAppByUserIdAndWorkspaceId(
      long userId, long workspaceId);

  void deleteDbUserWorkspaceAppByAppName(String appName);
}
