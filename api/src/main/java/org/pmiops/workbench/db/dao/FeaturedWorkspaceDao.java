package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface FeaturedWorkspaceDao extends CrudRepository<DbFeaturedWorkspace, Long> {
  Optional<DbFeaturedWorkspace> findByWorkspace(DbWorkspace workspace);

  List<DbFeaturedWorkspace> findDbFeaturedWorkspacesByCategory(
      DbFeaturedWorkspace.DbFeaturedCategory category);

  void deleteDbFeaturedWorkspaceByWorkspace(DbWorkspace workspace);
}
