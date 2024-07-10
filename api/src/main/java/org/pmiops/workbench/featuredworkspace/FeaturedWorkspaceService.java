package org.pmiops.workbench.featuredworkspace;

import java.util.List;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface FeaturedWorkspaceService {

  boolean isFeaturedWorkspace(DbWorkspace dbWorkspace);

  FeaturedWorkspaceCategory getFeaturedCategory(DbWorkspace dbWorkspace);

  /**
   * Get all Workspaces with entries in the Featured Workspace table. Use {@link
   * FeaturedWorkspaceDao#findAll()} to access the featured workspace rows themselves.
   */
  List<DbWorkspace> getDbWorkspaces();

  List<WorkspaceResponse> getFeaturedWorkspaces();
}
