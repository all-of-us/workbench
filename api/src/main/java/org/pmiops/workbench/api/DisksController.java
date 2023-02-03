package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListDisksResponse;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DisksController implements DisksApiDelegate {
  private static final Logger log = Logger.getLogger(DisksController.class.getName());

  // https://github.com/DataBiosphere/leonardo/blob/3774547f2018e056e9af42142a10ac004cfe1ee8/core/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/diskModels.scala#L60
  private static final Set<DiskStatus> ACTIVE_DISK_STATUSES =
      ImmutableSet.of(DiskStatus.READY, DiskStatus.CREATING, DiskStatus.RESTORING);

  private final LeonardoApiClient leonardoNotebooksClient;
  private final LeonardoMapper leonardoMapper;
  private final WorkspaceService workspaceService;
  private final Provider<DbUser> userProvider;

  @Autowired
  public DisksController(
      LeonardoApiClient leonardoNotebooksClient,
      LeonardoMapper leonardoMapper,
      Provider<DbUser> userProvider,
      WorkspaceService workspaceService) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.leonardoMapper = leonardoMapper;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Disk> getDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    Disk disk =
        leonardoMapper.toApiGetDiskResponse(
            leonardoNotebooksClient.getPersistentDisk(googleProject, diskName));

    if (DiskStatus.FAILED.equals(disk.getStatus())) {
      log.warning(
          String.format("Observed failed PD %s in workspace %s", diskName, workspaceNamespace));
    }

    return ResponseEntity.ok(disk);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.deletePersistentDisk(googleProject, diskName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateDisk(
      String workspaceNamespace, String diskName, Integer diskSize) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.updatePersistentDisk(googleProject, diskName, diskSize);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ListDisksResponse> listDisksInWorkspace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();

    List<LeonardoListPersistentDiskResponse> responseList =
        leonardoNotebooksClient.listPersistentDiskByProjectCreatedByCreator(googleProject, false);

    List<Disk> diskList =
        findTheMostRecentActiveDisks(
            responseList.stream()
                .map(leonardoMapper::toApiListDisksResponse)
                .collect(Collectors.toList()));
    ListDisksResponse listDisksResponse = new ListDisksResponse();
    listDisksResponse.addAll(diskList);

    return ResponseEntity.ok(listDisksResponse);
  }

  /**
   * Finds the most recent disks for all apps and GCE runtime.
   *
   * <p>We use {@link Disk#getCreatedDate} as the most recent disk.
   */
  private List<Disk> findTheMostRecentActiveDisks(List<Disk> disksToValidate) {
    // Iterate original list first to check if disks are valid. Print log if disks maybe in
    // incorrect state to help future debugging.
    // Disk maybe in incorrect state if having additional active state disks.

    List<Disk> activeDisks =
        disksToValidate.stream()
            .filter(
                d ->
                    ACTIVE_DISK_STATUSES.contains(d.getStatus()))
            .collect(Collectors.toList());
    if (activeDisks.size() > (AppType.values().length + 1)) {
      String diskNameList =
          activeDisks.stream().map(Disk::getName).collect(Collectors.joining(","));
      log.warning(String.format("Maybe incorrect disks: %s", diskNameList));
    }

    List<Disk> recentDisks = new ArrayList<>();
    // Find the runtime disk with maximum creation time.
    Optional<Disk> runtimeDisk =
        activeDisks.stream()
            .filter(Disk::getIsGceRuntime)
            .max(Comparator.comparing((r) -> Instant.parse(r.getCreatedDate())));
    runtimeDisk.ifPresent(recentDisks::add);

    // For each app type, find the disk with maximum creation time.
    Map<AppType, Disk> appDisks =
        activeDisks.stream()
            .filter(d -> d.getAppType() != null)
            .collect(
                Collectors.toMap(
                    Disk::getAppType,
                    Function.identity(),
                    BinaryOperator.maxBy(
                        Comparator.comparing((r) -> Instant.parse(r.getCreatedDate())))));
    recentDisks.addAll(appDisks.values());
    return recentDisks;
  }
}
