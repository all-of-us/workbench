package org.pmiops.workbench.cdrselector;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;

// This is a service for accessing objects that describe selections of CDR information. For
// example, datasets and cohorts, both of which define subsets of the CDR.
public interface WorkspaceResourcesService {
  public List<WorkspaceResource> getWorkspaceResources(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel, List<ResourceType> resourceTypes);
}
