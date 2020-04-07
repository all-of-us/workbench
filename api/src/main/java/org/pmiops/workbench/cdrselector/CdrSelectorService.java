package org.pmiops.workbench.cdrselector;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;

import java.util.List;

public interface CdrSelectorService {
  public List<WorkspaceResource> getCdrSelectorsInWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);
}
