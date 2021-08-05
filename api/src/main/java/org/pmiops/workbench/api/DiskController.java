package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiskController implements DiskApiDelegate {
  private static final Logger log = Logger.getLogger(DiskController.class.getName());
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final WorkspaceDao workspaceDao;
  private final LeonardoMapper leonardoMapper;
  private final Provider<DbUser> userProvider;

  @Autowired
  public DiskController(
      LeonardoNotebooksClient leonardoNotebooksClient,
      WorkspaceDao workspaceDao,
      Provider<DbUser> userProvider,
      LeonardoMapper leonardoMapper) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.workspaceDao = workspaceDao;
    this.userProvider = userProvider;
    this.leonardoMapper = leonardoMapper;
  }

  @Override
  public ResponseEntity<Disk> getDisk(String workspaceNamespace) {
    String googleProject = lookupWorkspace(workspaceNamespace).getGoogleProject();
    try {
      LeonardoListPersistentDiskResponse response;
      String pdNamePrefix = userProvider.get().getUserPDNamePrefix();
      List<LeonardoListPersistentDiskResponse> responseList =
          leonardoNotebooksClient.listPersistentDiskByProject(googleProject, false).stream()
              .filter(r -> r.getName().startsWith(pdNamePrefix))
              .sorted((r1, r2) -> r2.getId().compareTo(r1.getId())) // sort by descent order
              .collect(Collectors.toList());

      if (responseList.isEmpty()) {
        return ResponseEntity.notFound().build();
      }
      if (responseList.size() > 1) {
        log.warning(String.format("Observed multiple active PDs for a single user"));
      }
      response = responseList.get(0);
      if (LeonardoDiskStatus.FAILED.equals(response.getStatus())) {
        log.warning(
            String.format(
                "Observed Leonardo persistent disk with unexpected failed status:\n%s",
                response.getStatus()));
      }
      return ResponseEntity.ok(leonardoMapper.toApiDisk(response));

    } catch (NotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace, String diskName) {
    String googleProject = lookupWorkspace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.deletePersistentDisk(googleProject, diskName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateDisk(
      String workspaceNamespace, String diskName, Integer diskSize) {
    String googleProject = lookupWorkspace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.updatePersistentDisk(googleProject, diskName, diskSize);
    return ResponseEntity.ok(new EmptyResponse());
  }

  private DbWorkspace lookupWorkspace(String workspaceNamespace) throws NotFoundException {
    return workspaceDao
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }
}
