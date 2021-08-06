package org.pmiops.workbench.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
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
  // timestamp for the date was created in ISO 8601 format
  private static final String diskCreatedDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'";

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
    String pdNamePrefix = userProvider.get().getUserPDNamePrefix();
    SimpleDateFormat dateFormatter = new SimpleDateFormat(diskCreatedDateFormat);
    AtomicInteger activePDCnt = new AtomicInteger();
    AtomicInteger failedPDCnt = new AtomicInteger();
    Stream<LeonardoListPersistentDiskResponse> responseStream =
        leonardoNotebooksClient.listPersistentDiskByProject(googleProject, false).stream()
            .filter(r -> r.getName().startsWith(pdNamePrefix))
            .peek(
                r -> {
                  switch (r.getStatus()) {
                    case READY:
                      activePDCnt.addAndGet(1);
                      break;
                    case FAILED:
                      failedPDCnt.addAndGet(1);
                      break;
                  }
                }) // collect disk status
            .sorted(
                (r1, r2) -> {
                  try {
                    return dateFormatter
                        .parse(r2.getAuditInfo().getCreatedDate())
                        .compareTo(dateFormatter.parse(r1.getAuditInfo().getCreatedDate()));
                  } catch (ParseException e) {
                    log.warning(String.format("Observed PDs CreatedDate parsing error: %s", e));
                    return 0;
                  }
                }); // sort by descent order

    LeonardoListPersistentDiskResponse response =
        responseStream.findFirst().orElseThrow(NotFoundException::new);

    if (activePDCnt.get() > 1) {
      log.warning(
          String.format(
              "Observed multiple active PDs for a single user with prefix %s in workspace %s",
              pdNamePrefix, workspaceNamespace));
    }
    if (failedPDCnt.get() > 0) {
      log.warning(
          String.format(
              "Observed failed PDs with prefix %s in workspace %s",
              pdNamePrefix, workspaceNamespace));
    }

    return ResponseEntity.ok(leonardoMapper.toApiDisk(response));
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
