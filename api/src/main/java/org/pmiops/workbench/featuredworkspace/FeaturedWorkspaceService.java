package org.pmiops.workbench.featuredworkspace;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;

public interface FeaturedWorkspaceService {
  Optional<FeaturedWorkspaceCategory> getFeaturedCategory(DbWorkspace dbWorkspace);

  void backFillFeaturedWorkspaces();

  List<WorkspaceResponse> getWorkspaceResponseByFeaturedCategory(
      FeaturedWorkspaceCategory featuredWorkspaceCategory);
}
