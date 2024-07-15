package org.pmiops.workbench.featuredworkspace;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;

public interface FeaturedWorkspaceService {

  // TODO: not sure if we need this, if we have the other
  boolean isFeaturedWorkspace(DbWorkspace dbWorkspace);

  Optional<FeaturedWorkspaceCategory> getFeaturedCategory(DbWorkspace dbWorkspace);
}
