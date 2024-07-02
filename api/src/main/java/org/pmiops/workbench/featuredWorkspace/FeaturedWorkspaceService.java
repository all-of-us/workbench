package org.pmiops.workbench.featuredWorkspace;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;

public interface FeaturedWorkspaceService {

  boolean isFeaturedWorkspace(DbWorkspace dbWorkspace);

  FeaturedWorkspaceCategory getFeaturedCategory(DbWorkspace dbWorkspace);
}
