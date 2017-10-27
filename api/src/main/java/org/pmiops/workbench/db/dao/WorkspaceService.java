package org.pmiops.workbench.db.dao;

import java.util.logging.Logger;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao or @Query annotations.
 *
 * TODO(RW-215) Add versioning to detect/prevent concurrent edits.
 */
@Service
public class WorkspaceService {
  private static final Logger log = Logger.getLogger(WorkspaceService.class.getName());

  /**
   * Clients wishing to use the auto-generated methods from the DAO interface may directly access
   * it here.
   */
  public final WorkspaceDao dao;

  @Autowired
  public WorkspaceService(WorkspaceDao workspaceDao) {
    this.dao = workspaceDao;
  }

  public Workspace getRequired(String ns, String id) {
    Workspace workspace = dao.get(ns, id);
    if (workspace == null) {
      throw new NotFoundException(String.format("Workspace %s/%s not found.", ns, id));
    }
    return workspace;
  }

  // FIXME @Version instead? Bean instantiation v. @Transactional?
  //@Transactional
  public void setResearchPurposeApproved(String ns, String id, boolean approved) {
    Workspace workspace = getRequired(ns, id);
    if (workspace.getReviewRequested() == null || !workspace.getReviewRequested()) {
      throw new BadRequestException(String.format(
          "No review requested for workspace %s/%s.", ns, id));
    }
    if (workspace.getApproved() != null) {
      throw new BadRequestException(String.format(
          "Workspace %s/%s already %s.",
          ns, id, workspace.getApproved() ? "approved" : "rejected"));
    }
    workspace.setApproved(approved);
    dao.save(workspace);
  }
}
