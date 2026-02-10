package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspaceBucketArchive;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceBucketArchiveDao
    extends CrudRepository<DbWorkspaceBucketArchive, Long> {
  List<DbWorkspaceBucketArchive> findByLegacyWorkspaceId(Long legacyWorkspaceId);
}
