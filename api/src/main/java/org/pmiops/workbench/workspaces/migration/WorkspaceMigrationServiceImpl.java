package org.pmiops.workbench.workspaces.migration;

import com.google.storagetransfer.v1.proto.TransferTypes.TransferOperation;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig.CdrVersionForMigration;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceBucketArchiveDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.google.StorageTransferClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.WorkbenchStringUtils;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.UserActiveState;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.CreatedControlledGcpGcsBucket;
import org.pmiops.workbench.wsmanager.model.IamRole;
import org.pmiops.workbench.wsmanager.model.Property;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMigrationServiceImpl implements WorkspaceMigrationService {

  private static final Logger logger =
      Logger.getLogger(WorkspaceMigrationServiceImpl.class.getName());
  private final WsmClient wsmClient;
  private final WorkspaceDao workspaceDao;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final StorageTransferClient storageTransferClient;
  private final TaskQueueService taskQueueService;
  private final VwbUserService vwbUserService;
  private final MailService mailService;
  private final Provider<DbUser> userProvider;
  private final Clock clock;
  private final WorkspaceBucketArchiveDao workspaceBucketArchiveDao;
  private final ActionAuditQueryService actionAuditQueryService;

  @Autowired
  public WorkspaceMigrationServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      StorageTransferClient storageTransferClient,
      TaskQueueService taskQueueService,
      VwbUserService vwbUserService,
      MailService mailService,
      Provider<DbUser> userProvider,
      Clock clock,
      WorkspaceBucketArchiveDao workspaceBucketArchiveDao,
      ActionAuditQueryService actionAuditQueryService) {

    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.storageTransferClient = storageTransferClient;
    this.taskQueueService = taskQueueService;
    this.vwbUserService = vwbUserService;
    this.mailService = mailService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.workspaceBucketArchiveDao = workspaceBucketArchiveDao;
    this.actionAuditQueryService = actionAuditQueryService;
  }

  @Override
  public void requestWorkspaceRecovery(String namespace, String podId) {
    logger.log(Level.INFO, namespace + ": Requesting workspace recovery");

    DbWorkspace dbWorkspace = workspaceDao.findByWorkspaceNamespace(namespace);

    // Validate workspace is eligible for recovery
    if (!WorkspaceRecoveryStatus.NOT_STARTED.toString().equals(dbWorkspace.getRecoveryState())) {
      throw new RuntimeException(
          namespace
              + ": Recovery has already been requested or is in progress. Current state="
              + dbWorkspace.getRecoveryState());
    }

    // Validate archive exists
    List<DbWorkspaceBucketArchive> archives =
        workspaceBucketArchiveDao.findByLegacyWorkspaceId(dbWorkspace.getWorkspaceId());

    if (archives.isEmpty()) {
      throw new RuntimeException(namespace + ": Archive metadata not found");
    }

    DbWorkspaceBucketArchive archive = archives.get(0);

    if (!WorkspaceArchiveStatus.ARCHIVED.toString().equals(archive.getStatus())) {
      throw new RuntimeException(
          namespace + ": Workspace is not archived. Current state=" + archive.getStatus());
    }

    // Update recovery state to REQUESTED and set recoveryPodId
    dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.REQUESTED.name());
    dbWorkspace.setRecoveryPodId(podId);
    dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    workspaceDao.save(dbWorkspace);

    // Get workspace owner
    DbUser owner = dbWorkspace.getCreator();
    if (owner == null) {
      throw new RuntimeException(namespace + ": Workspace owner not found");
    }

    // Send recovery request email to admin/support
    try {
      mailService.sendWorkspaceRecoveryRequestEmail(dbWorkspace, owner, userProvider.get());
    } catch (MessagingException e) {
      logger.log(
          Level.WARNING, namespace + ": Failed to send recovery request email: " + e.getMessage());
      // Don't fail the request if email sending fails
    }

    logger.log(Level.INFO, namespace + ": Workspace recovery request submitted");
  }

  @Override
  public void startWorkspaceRecovery(String namespace, String researchPurpose, String podId) {

    Duration bucketDelay = Duration.ofSeconds(10);

    DbWorkspace dbWorkspace = workspaceDao.findByWorkspaceNamespace(namespace);

    logger.log(Level.INFO, namespace + ": Starting workspace recovery");

    // Validate recovery state is REQUESTED or FAILED before starting
    if (!(WorkspaceRecoveryStatus.REQUESTED.toString().equals(dbWorkspace.getRecoveryState())
        || WorkspaceRecoveryStatus.FAILED.toString().equals(dbWorkspace.getRecoveryState()))) {
      throw new RuntimeException(
          namespace
              + ": Workspace recovery can only start when state is REQUESTED or FAILED. Current state="
              + dbWorkspace.getRecoveryState());
    }

    try {

      List<DbWorkspaceBucketArchive> archives =
          workspaceBucketArchiveDao.findByLegacyWorkspaceId(dbWorkspace.getWorkspaceId());

      if (archives.isEmpty()) {
        throw new RuntimeException(namespace + ": Archive metadata not found");
      }

      DbWorkspaceBucketArchive archive = archives.get(0);

      if (!WorkspaceArchiveStatus.ARCHIVED.toString().equals(archive.getStatus())) {

        throw new RuntimeException(
            namespace + ": Workspace is not archived. Current state=" + archive.getStatus());
      }

      logger.log(Level.INFO, namespace + ": Recovery source=" + archive.getGcsPath());

      dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.RECOVERING.name());

      dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));

      workspaceDao.save(dbWorkspace);

      String gcsPath = archive.getGcsPath().replace("gs://", "");

      String[] pathParts = gcsPath.split("/", 2);

      String archiveBucket = pathParts[0];

      String archivePrefix = pathParts.length > 1 ? pathParts[1] : "";

      logger.log(
          Level.INFO, namespace + ": Archive bucket=" + archiveBucket + " prefix=" + archivePrefix);

      DbUser creator = userDao.findUserByUserId(dbWorkspace.getCreator().getUserId());

      String resolvedPodId =
          podId != null
              ? podId
              : Optional.ofNullable(creator)
                  .map(DbUser::getVwbUserPod)
                  .map(DbVwbUserPod::getVwbPodId)
                  .orElse(workbenchConfigProvider.get().vwb.defaultPodId);

      logger.log(Level.INFO, namespace + ": Creating new recovery workspace");

      WorkspaceDescription vwbWorkspace =
          wsmClient.createWorkspaceAsService(dbWorkspace, resolvedPodId);

      UUID workspaceId = vwbWorkspace.getId();

      dbWorkspace.setMigratedVwbWorkspaceId(workspaceId.toString());

      workspaceDao.save(dbWorkspace);

      wsmClient.shareWorkspaceAsService(
          workspaceId.toString(), creator.getUsername(), IamRole.OWNER);

      logger.log(
          Level.INFO, namespace + ": Fetching existing collaborators from action audit data");

      List<ActionAuditQueryService.UserIdWithRoleImpl> collaborators =
          actionAuditQueryService.getWorkspaceUsersById(dbWorkspace.getWorkspaceId());
      if (collaborators != null) {
        collaborators.forEach(
            c -> {
              DbUser collaborator = userDao.findUserByUserId(c.userId());
              if (collaborator == null) {
                return;
              }
              String collaboratorEmail = collaborator.getUsername();

              // Skip creator, already shared above
              if (collaboratorEmail.equals(creator.getUsername())) {
                return;
              }

              try {
                OrganizationMember member = vwbUserService.getOrganizationMember(collaboratorEmail);

                // Skip if not found in VWB
                if (member == null || member.getUserDescription() == null) {
                  logger.log(
                      Level.INFO,
                      namespace + ": Skipping collaborator not found in VWB: " + collaboratorEmail);
                  return;
                }

                // Skip if not ENABLED (could be INVITED, DECLINED, DISABLED, ARCHIVED)
                if (!UserActiveState.ENABLED.equals(member.getUserDescription().getActiveState())) {
                  logger.log(
                      Level.INFO,
                      namespace
                          + ": Skipping inactive collaborator: "
                          + collaboratorEmail
                          + " state: "
                          + member.getUserDescription().getActiveState());
                  return;
                }

                // Map Terra role to VWB IamRole
                IamRole vwbRole = mapTerraRoleToVwbRole(c.role());
                if (vwbRole == null) {
                  logger.log(
                      Level.INFO,
                      namespace
                          + ": Skipping collaborator with unmappable role: "
                          + collaboratorEmail);
                  return;
                }

                logger.log(
                    Level.INFO,
                    namespace + ": Sharing workspace with collaborator: " + collaboratorEmail);
                wsmClient.shareWorkspaceAsService(
                    workspaceId.toString(), collaboratorEmail, vwbRole);

              } catch (Exception e) {
                // Don't fail entire migration for one collaborator
                logger.log(
                    Level.WARNING,
                    namespace + ": Failed to share with collaborator: " + collaboratorEmail,
                    e);
              }
            });
      }

      List<Property> properties =
          List.of(
              new Property().key("terra-default-location").value("us-central1"),
              new Property()
                  .key("terra-required-data-use-metadata")
                  .value(WorkbenchStringUtils.encodeUserInput(researchPurpose)),
              new Property().key("terra-workspace-short-description").value(""));
      wsmClient.updateWorkspaceProperties(properties, workspaceId.toString());

      long cdrVersionId = dbWorkspace.getCdrVersion().getCdrVersionId();

      CdrVersionForMigration cdrVersionForMigration =
          workbenchConfigProvider.get().vwb.cdrVersionsForMigration.stream()
              .filter(c -> c.cdrVersionId == cdrVersionId)
              .findFirst()
              .orElse(null);
      String sourceWorkspaceId;
      String resourceId;
      if (cdrVersionForMigration == null) {
        // Outdated CDR version, set v9 ids
        if (dbWorkspace.getCdrVersion().getAccessTier().getShortName().equals("controlled")) {
          sourceWorkspaceId = "3d83ef80-77d7-43e8-a479-52946619b769";
          resourceId = "1a27006f-6aea-4a10-bdc1-c4d562d7828d";
        } else {
          sourceWorkspaceId = "698c6700-afbe-454a-b73a-c675e629336c";
          resourceId = "d6ac7edd-2fe6-4221-8680-2cf828665cd3";
        }
      } else {
        sourceWorkspaceId = cdrVersionForMigration.workspaceId;
        resourceId = cdrVersionForMigration.resourceId;
      }

      try {
        logger.log(Level.INFO, namespace + ": Starting BQ clone");
        wsmClient.cloneBQDataset(
            workspaceId,
            sourceWorkspaceId,
            UUID.fromString(resourceId),
            UUID.randomUUID().toString());

        logger.log(Level.INFO, namespace + ": BQ clone complete");
      } catch (Exception e) {
        throw new RuntimeException(namespace + ": BQ clone failed", e);
      }

      CreatedControlledGcpGcsBucket controlledBucket =
          wsmClient.createControlledBucket(workspaceId.toString(), namespace);

      Thread.sleep(bucketDelay.toMillis());

      WorkspaceDescription vwbWorkspaceWithPolicies =
          wsmClient.getWorkspaceAsService(vwbWorkspace.getUserFacingId());

      logger.log(
          Level.INFO, namespace + ": New workspace with policies: " + vwbWorkspaceWithPolicies);

      String destinationBucket = controlledBucket.getGcpBucket().getAttributes().getBucketName();

      String serviceAccountEmail = workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0);

      String projectId = workbenchConfigProvider.get().server.projectId;

      logger.log(Level.INFO, namespace + ": Creating recovery transfer");

      String jobName =
          storageTransferClient.createTransferJob(
              archiveBucket,
              archivePrefix,
              destinationBucket,
              null,
              "recovery-" + namespace,
              projectId,
              null,
              serviceAccountEmail,
              false);

      storageTransferClient.runTransferJob(projectId, jobName);

      logger.log(Level.INFO, namespace + ": Recovery transfer started");

      taskQueueService.pushWorkspaceRecoveryStatusTask(namespace);

    } catch (Exception e) {

      logger.log(Level.SEVERE, namespace + ": Recovery failed", e);

      dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.FAILED.name());

      dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));

      workspaceDao.save(dbWorkspace);

      throw new RuntimeException(namespace + ": Recovery failed to start", e);
    }
  }

  @Override
  public void checkRecoveryStatus(String namespace) {

    logger.log(Level.INFO, namespace + ": Checking recovery queue status");

    DbWorkspace dbWorkspace = workspaceDao.findByWorkspaceNamespace(namespace);

    String projectId = workbenchConfigProvider.get().server.projectId;

    String jobName = "transferJobs/migration-recovery-" + namespace;

    TransferOperation transferOperation =
        storageTransferClient.getTransferJobStatus(projectId, jobName);

    TransferOperation.Status jobStatus = transferOperation.getStatus();

    logger.log(Level.INFO, namespace + ": Recovery status=" + jobStatus);

    switch (jobStatus) {
      case IN_PROGRESS:
      case QUEUED:
        logger.log(Level.INFO, namespace + ": Recovery transfer in progress, requeue");
        taskQueueService.pushWorkspaceRecoveryStatusTask(namespace);

        return;

      case FAILED:
        logger.log(
            Level.INFO,
            namespace
                + ": Recovery transfer failed: "
                + transferOperation.getErrorBreakdownsList());
        dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.FAILED.name());

        workspaceDao.save(dbWorkspace);

        storageTransferClient.deleteTransferJob(projectId, jobName);

        return;

      case SUCCESS:
        logger.log(Level.INFO, namespace + ": Recovery transfer success");
        dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.RECOVERED.name());

        workspaceDao.save(dbWorkspace);
        List<ActionAuditQueryService.UserIdWithRoleImpl> collaborators =
            actionAuditQueryService.getWorkspaceUsersById(dbWorkspace.getWorkspaceId());
        List<DbUser> owners =
            collaborators.stream()
                .filter(c -> "OWNER".equals(c.role()))
                .map(c -> userDao.findUserByUserId(c.userId()))
                .toList();
        try {
          mailService.sendWorkspaceUnarchivedEmail(dbWorkspace, owners);
        } catch (MessagingException e) {
          logger.log(Level.WARNING, "Failed to send workspace unarchive email", e);
        }

        storageTransferClient.deleteTransferJob(projectId, jobName);

        logger.log(Level.INFO, namespace + ": Recovery completed");

        return;
    }
  }

  private IamRole mapTerraRoleToVwbRole(String terraAccessLevel) {
    if (terraAccessLevel == null) return null;
    return switch (terraAccessLevel) {
      case "OWNER" -> IamRole.OWNER;
      case "WRITER" -> IamRole.WRITER;
      case "READER" -> IamRole.READER;
      default -> {
        logger.log(Level.WARNING, "Unknown Terra access level: " + terraAccessLevel);
        yield null;
      }
    };
  }
}
