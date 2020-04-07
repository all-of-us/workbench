package org.pmiops.workbench.cdrselector;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;

public interface CdrSelectorService {
  public List<WorkspaceResource> getCdrSelectorsInWorkspace(
      DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);
}
