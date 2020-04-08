package org.pmiops.workbench.cdrselector;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;

// This is a service for accessing objects that describe selections of CDR information. For
// example, datasets and cohorts, both of which define subsets of the CDR.
public interface CdrSelectorService {
  public List<WorkspaceResource> getCdrSelectorsInWorkspace(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);
}
