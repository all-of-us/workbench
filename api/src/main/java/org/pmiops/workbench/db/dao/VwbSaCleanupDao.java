package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbVwbSaCleanup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface VwbSaCleanupDao extends CrudRepository<DbVwbSaCleanup, Long> {
  @Query("SELECT workspaceId FROM DbVwbSaCleanup")
  List<String> findAllWorkspaceIds();
}
