package org.pmiops.workbench.workspaces;

import jakarta.inject.Provider;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.TemporaryInitialCreditsRelinkWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbTemporaryInitialCreditsRelinkWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.model.Workspace;
import org.springframework.stereotype.Service;

@Service
public class TemporaryInitialCreditsRelinkServiceImpl
    implements TemporaryInitialCreditsRelinkService {
  private static final Logger log =
      Logger.getLogger(TemporaryInitialCreditsRelinkServiceImpl.class.getName());

  private final FireCloudService fireCloudService;
  private final CloudBillingClient cloudBillingClient;
  private final WorkspaceDao workspaceDao;
  private final TemporaryInitialCreditsRelinkWorkspaceDao relinkWorkspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public TemporaryInitialCreditsRelinkServiceImpl(
      FireCloudService fireCloudService,
      CloudBillingClient cloudBillingClient,
      WorkspaceDao workspaceDao,
      TemporaryInitialCreditsRelinkWorkspaceDao relinkWorkspaceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.fireCloudService = fireCloudService;
    this.cloudBillingClient = cloudBillingClient;
    this.workspaceDao = workspaceDao;
    this.relinkWorkspaceDao = relinkWorkspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Relink the initial credits billing account to the source workspace and record temporary
   * relinking for future cleanup.
   */
  @Override
  public void initiateTemporaryRelinking(
      DbWorkspace sourceWorkspace, Workspace destinationWorkspace) {
    String initialCreditsBillingAccountName =
        workbenchConfigProvider.get().billing.initialCreditsBillingAccountName();
    log.info(
        String.format(
            "Source workspace has exhausted/expired initial credits. "
                + "Temporarily relinking to initial credits billing account for duplication. [sourceWorkspaceNamespace=%s]",
            sourceWorkspace.getWorkspaceNamespace()));
    var saved =
        relinkWorkspaceDao.save(
            new DbTemporaryInitialCreditsRelinkWorkspace()
                .setSourceWorkspaceId(sourceWorkspace.getWorkspaceId())
                .setDestinationWorkspaceNamespace(destinationWorkspace.getNamespace()));
    try {
      fireCloudService.updateBillingAccountAsService(
          sourceWorkspace.getWorkspaceNamespace(), initialCreditsBillingAccountName);
      cloudBillingClient.pollUntilBillingAccountLinked(
          sourceWorkspace.getGoogleProject(), initialCreditsBillingAccountName);
    } catch (IOException | InterruptedException e) {
      log.log(
          Level.WARNING,
          String.format(
              "Failed to temporarily relink source workspace to initial credits billing account. [sourceWorkspaceNamespace=%s]",
              sourceWorkspace.getWorkspaceNamespace()),
          e);
      relinkWorkspaceDao.delete(saved);
      throw new ServerErrorException(
          "Timed out while preparing source workspace for duplication.", e);
    }
  }

  /**
   * Find all active clones and check if they're done cloning. If they are, remove billing from
   * their respective source workspace as long as there are no other workspaces actively cloning the
   * same source workspace. Mark the finished clones as complete in the database.
   */
  @Override
  public void cleanupTemporarilyRelinkedWorkspaces() {
    var workspacesToCheck = relinkWorkspaceDao.findByCloneCompletedIsNull();

    var workspacesWithCloneCompletedTimes =
        workspacesToCheck.stream().map(this::getWorkspaceCloneCompletedTime).toList();

    var partitioned =
        workspacesWithCloneCompletedTimes.stream()
            .collect(Collectors.partitioningBy(ws -> ws.cloneCompleted.isPresent()));

    var completedWorkspaces = partitioned.get(true);

    List<WorkspaceWithCloneCompletedTime> inProgressWorkspaces = partitioned.get(false);
    var inProgressWorkspaceSourceIds =
        inProgressWorkspaces.stream()
            .map(ic -> ic.workspace.getSourceWorkspaceId())
            .collect(Collectors.toSet());

    for (var completedWorkspace : completedWorkspaces) {
      try {
        var sourceId = completedWorkspace.workspace.getSourceWorkspaceId();
        // Check for in progress clones from the same source workspace before attempting to unlink
        // billing to avoid interrupting other in progress clones
        if (!inProgressWorkspaceSourceIds.contains(sourceId)) {
          Optional<DbWorkspace> sourceWorkspace = workspaceDao.findActiveByWorkspaceId(sourceId);
          sourceWorkspace.ifPresent(
              ws ->
                  fireCloudService.removeBillingAccountFromBillingProjectAsService(
                      ws.getWorkspaceNamespace()));
        }

        completedWorkspace.workspace.setCloneCompleted(
            Timestamp.from(
                completedWorkspace
                    .cloneCompleted
                    .get() // safe because we partition by cloneCompleted.isPresent() above
                    .toInstant()));
        relinkWorkspaceDao.save(completedWorkspace.workspace);
      } catch (Exception e) {
        log.log(
            Level.SEVERE,
            String.format(
                "Failed to cleanup temporary relinking record. [temporaryRelinkWorkspaceRecordId=%d]",
                completedWorkspace.workspace.getId()),
            e);
      }
    }
  }

  record WorkspaceWithCloneCompletedTime(
      DbTemporaryInitialCreditsRelinkWorkspace workspace,
      Optional<OffsetDateTime> cloneCompleted) {}

  /** Return workspace with optional cloneCompleted timestamp from Terra getWorkspace response. */
  private WorkspaceWithCloneCompletedTime getWorkspaceCloneCompletedTime(
      DbTemporaryInitialCreditsRelinkWorkspace workspace) {
    var dbWorkspace = workspaceDao.getByNamespace(workspace.getDestinationWorkspaceNamespace());

    // If the cloneOperation was started over an hour ago and there's no record in the database or
    // Terra, something went wrong. We'll mark it as completed and cleanup billing as needed. We
    // need to wait for a bit to avoid a race condition as the temporary relinking record is created
    // before the workspace database record or the Terra workspace are created.
    var timeoutThreshold = Timestamp.from(Instant.now().minus(Duration.ofHours(1)));

    if (dbWorkspace.isEmpty() && workspace.getCreated().before(timeoutThreshold)) {
      log.warning(
          String.format(
              "Destination workspace for temporary initial credits relink not found in DB. Marking relink record as completed. [temporaryRelinkWorkspaceRecordId=%d]",
              workspace.getId()));
      return new WorkspaceWithCloneCompletedTime(workspace, Optional.of(OffsetDateTime.now()));
    }

    Optional<OffsetDateTime> cloneCompleted;
    try {
      var fcWorkspace =
          dbWorkspace.map(
              dbWs ->
                  fireCloudService.getWorkspaceAsService(
                      dbWs.getWorkspaceNamespace(), dbWs.getFirecloudName()));
      cloneCompleted =
          fcWorkspace.flatMap(
              fcWs ->
                  Optional.ofNullable(
                      fcWs.getWorkspace().getCompletedCloneWorkspaceFileTransfer()));
    } catch (Exception e) {
      if (workspace.getCreated().before(timeoutThreshold)) {
        log.log(
            Level.WARNING,
            String.format(
                "Error retrieving Terra workspace for temporary initial credits relink. Marking relink record as completed. [temporaryRelinkWorkspaceRecordId=%d]",
                workspace.getId()),
            e);
        cloneCompleted = Optional.of(OffsetDateTime.now());
      } else {
        cloneCompleted = Optional.empty();
      }
    }

    return new WorkspaceWithCloneCompletedTime(workspace, cloneCompleted);
  }
}
