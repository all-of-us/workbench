package org.pmiops.workbench.featuredworkspace;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;

public interface FeaturedWorkspaceService {
  Optional<FeaturedWorkspaceCategory> getFeaturedCategory(DbWorkspace dbWorkspace);
}
