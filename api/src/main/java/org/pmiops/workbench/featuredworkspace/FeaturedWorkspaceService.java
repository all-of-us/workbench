package org.pmiops.workbench.featuredworkspace;

import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;

public interface FeaturedWorkspaceService {

  boolean isFeaturedWorkspace(DbWorkspace dbWorkspace);

  FeaturedWorkspaceCategory getFeaturedCategory(DbWorkspace dbWorkspace);

  Iterable<DbFeaturedWorkspace> getFeaturedWorkspaces();
}
