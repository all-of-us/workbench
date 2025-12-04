package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.TemporaryInitialCreditsRelinkWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.springframework.stereotype.Service;

@Service
public class TemporaryInitialCreditsRelinkServiceImpl
    implements TemporaryInitialCreditsRelinkService {
  private static final Logger log =
      Logger.getLogger(TemporaryInitialCreditsRelinkServiceImpl.class.getName());

  private final FireCloudService fireCloudService;
  private final WorkspaceDao workspaceDao;
  private final TemporaryInitialCreditsRelinkWorkspaceDao relinkWorkspaceDao;

  public TemporaryInitialCreditsRelinkServiceImpl(
      FireCloudService fireCloudService,
      WorkspaceDao workspaceDao,
      TemporaryInitialCreditsRelinkWorkspaceDao relinkWorkspaceDao) {
    this.fireCloudService = fireCloudService;
    this.workspaceDao = workspaceDao;
    this.relinkWorkspaceDao = relinkWorkspaceDao;
  }

  @Override
  public void cleanupTemporarilyRelinkedWorkspaces() {
    // - find all active clones
    // - check if they're done cloning
    // - if they are:
    //     - if there are no other clones of the same source workspace:
    //         - remove billing from source workspace
    //     - mark the clone as complete

    var workspacesToCheck = relinkWorkspaceDao.findByCloneCompletedIsNull();
    for (var workspace : workspacesToCheck) {
      var destinationWorkspaces =
          workspaceDao.findAllByWorkspaceNamespace(workspace.getDestinationWorkspaceNamespace());
      if (destinationWorkspaces == null || destinationWorkspaces.size() != 1) {
        log.warning(
            String.format(
                "Workspace namespace %s returned an unexpected number of workspaces.",
                workspace.getDestinationWorkspaceNamespace()));
      } else {
        var destinationWorkspace = destinationWorkspaces.iterator().next();
        var firecloudWorkspace =
            fireCloudService.getWorkspaceAsService(
                destinationWorkspace.getWorkspaceNamespace(),
                destinationWorkspace.getFirecloudName());
        if (firecloudWorkspace.getWorkspace().getCompletedCloneWorkspaceFileTransfer() != null) {
          if (workspacesToCheck.stream()
              .anyMatch(
                  ws ->
                      ws != workspace
                          && ws.getSourceWorkspaceNamespace()
                              .equals(workspace.getSourceWorkspaceNamespace()))) {
            fireCloudService.removeBillingAccountFromBillingProjectAsService(
                workspace.getSourceWorkspaceNamespace());
            pollUntilUnlinked(workspace.getSourceWorkspaceNamespace());
          }
          workspace.setCloneCompleted(
              Timestamp.from(
                  firecloudWorkspace
                      .getWorkspace()
                      .getCompletedCloneWorkspaceFileTransfer()
                      .toInstant()));
          relinkWorkspaceDao.save(workspace);
        }
      }
    }
  }

  private void pollUntilUnlinked(String workspaceNamespace) {}
}
