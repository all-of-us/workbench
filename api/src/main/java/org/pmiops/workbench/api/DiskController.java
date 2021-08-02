package org.pmiops.workbench.api;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.leonardo.model.LeonardoGetPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiskController implements DiskApiDelegate {
  private static final Logger log = Logger.getLogger(DiskController.class.getName());
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceAuthService workspaceAuthService;
  private final LeonardoMapper leonardoMapper;

  @Autowired
  public DiskController(
      LeonardoNotebooksClient leonardoNotebooksClient,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      LeonardoMapper leonardoMapper) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.leonardoMapper = leonardoMapper;
  }

  private DbWorkspace lookupWorkspace(String workspaceNamespace)
      throws org.pmiops.workbench.exceptions.NotFoundException {
    return workspaceDao
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }

  @Override
  public ResponseEntity<Disk> getDisk(String workspaceNamespace) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String googleProject = dbWorkspace.getGoogleProject();
    try {
      LeonardoGetPersistentDiskResponse response;
      List<LeonardoListPersistentDiskResponse> responseList =
          leonardoNotebooksClient.listPersistentDiskByProject(googleProject, false);
      responseList.sort(
          new Comparator<LeonardoListPersistentDiskResponse>() {
            @Override
            public int compare(
                LeonardoListPersistentDiskResponse t1, LeonardoListPersistentDiskResponse t2) {
              return -1 * t1.getId().compareTo(t2.getId());
            }
          });
      response =
          !responseList.isEmpty()
              ? leonardoNotebooksClient.getPersistentDisk(
                  googleProject, responseList.get(0).getName())
              : null;

      if (response != null && LeonardoRuntimeStatus.ERROR.equals(response.getStatus())) {
        log.warning(
            String.format(
                "Observed Leonardo runtime with unexpected error status:\n%s",
                response.getStatus()));
      }

      if (response != null) {
        return ResponseEntity.ok(leonardoMapper.toApiDisk(response));

      } else {
        return ResponseEntity.ok(null);
      }

    } catch (NotFoundException e) {
      return (ResponseEntity<Disk>) ResponseEntity.notFound();
    }
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    Disk disk = getDisk(workspaceNamespace).getBody();
    if (disk != null) {
      leonardoNotebooksClient.deletePersistentDisk(dbWorkspace.getGoogleProject(), disk.getName());
    } else {
      log.warning(
          String.format(
              "No existing persistent disk could be deleted in workspace \n%s",
              workspaceNamespace));
    }

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateDisk(
      String workspaceNamespace, String diskName, Integer diskSize) {
    DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
    leonardoNotebooksClient.updatePersistentDisk(
        dbWorkspace.getGoogleProject(), diskName, diskSize);
    return ResponseEntity.ok(new EmptyResponse());
  }
}
