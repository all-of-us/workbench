package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao or @Query annotations.
 */
public class WorkspaceService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access
   * it here.
   */
  public final WorkspaceDao dao;

  @Autowired
  WorkspaceService(WorkspaceDao workspaceDao) {
    this.dao = workspaceDao;
  }

  public Workspace get(String workspaceNamespace, String workspaceId) {
    return dao.findByWorkspaceNamespaceAndFirecloudName(workspaceNamespace, workspaceId);
  }

  public Workspace getRequired(String ns, String id) {
    Workspace workspace = get(ns, id);
    if (workspace == null) {
      throw new NotFoundException("Workspace {0}/{1} not found".format(ns, id));
    }
    return workspace;
  }

  public void setResearchPurposeApproved(String ns, String id, boolean approved) {
  }
}
