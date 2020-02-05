package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {

  private final WorkspaceDao workspaceDao;

  @Autowired
  public WorkspaceAdminServiceImpl(
      WorkspaceDao workspaceDao
  ) {
    this.workspaceDao = workspaceDao;
  }

  @Override
  /**
   * Returns the first workspace found for any given namespace.
   */
  public Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace) {
    return workspaceDao.findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(workspaceNamespace);
  }
}
