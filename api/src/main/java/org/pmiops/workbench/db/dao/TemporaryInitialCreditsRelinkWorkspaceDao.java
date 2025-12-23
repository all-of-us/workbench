package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbTemporaryInitialCreditsRelinkWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface TemporaryInitialCreditsRelinkWorkspaceDao
    extends CrudRepository<DbTemporaryInitialCreditsRelinkWorkspace, Long> {
  List<DbTemporaryInitialCreditsRelinkWorkspace> findByCloneCompletedIsNull();
}
