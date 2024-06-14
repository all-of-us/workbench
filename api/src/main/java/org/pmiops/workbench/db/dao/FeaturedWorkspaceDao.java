package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface FeaturedWorkspaceDao extends CrudRepository<DbFeaturedWorkspace, Long> {

  public DbFeaturedWorkspace findByWorkspace(DbWorkspace workspace);

  public List<DbFeaturedWorkspace> findAll();
}
