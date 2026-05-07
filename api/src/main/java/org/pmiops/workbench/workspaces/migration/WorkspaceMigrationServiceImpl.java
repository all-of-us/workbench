package org.pmiops.workbench.workspaces.migration;

import com.google.cloud.storage.Blob;
import com.google.storagetransfer.v1.proto.TransferTypes.TransferOperation;
import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig.CdrVersionForMigration;
import org.pmiops.workbench.db.dao.FolderSyncTransferDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFolderSyncTransfer;
import org.pmiops.workbench.db.model.DbFolderSyncTransfer.TransferState;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.StorageTransferClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.MigrationBucketContentsResponse;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.model.PreprodWorkspace;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.user.api.PodApi;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.PodAction;
import org.pmiops.workbench.vwb.user.model.UserActiveState;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.wsmanager.ApiException;
import org.pmiops.workbench.wsmanager.model.CreatedControlledGcpGcsBucket;
import org.pmiops.workbench.wsmanager.model.IamRole;
import org.pmiops.workbench.wsmanager.model.Property;
import org.pmiops.workbench.wsmanager.model.ResourceDescription;
import org.pmiops.workbench.wsmanager.model.ResourceList;
import org.pmiops.workbench.wsmanager.model.ResourceType;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMigrationServiceImpl implements WorkspaceMigrationService {

  private static final Logger logger =
      Logger.getLogger(WorkspaceMigrationServiceImpl.class.getName());
  private final WsmClient wsmClient;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final UserDao userDao;
  private final FolderSyncTransferDao folderSyncTransferDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final FireCloudService fireCloudService;
  private final InitialCreditsService initialCreditsService;
  private final CloudStorageClient cloudStorageClient;
  private final StorageTransferClient storageTransferClient;
  private final TaskQueueService taskQueueService;
  private final VwbUserService vwbUserService;
  private final MailService mailService;
  private final WorkspaceService workspaceService;
  private final Provider<PodApi> podApiProvider;
  private final Provider<DbUser> userProvider;
  private final Clock clock;

  @Autowired
  public WorkspaceMigrationServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      UserDao userDao,
      FolderSyncTransferDao folderSyncTransferDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      CloudStorageClient cloudStorageClient,
      StorageTransferClient storageTransferClient,
      TaskQueueService taskQueueService,
      VwbUserService vwbUserService,
      MailService mailService,
      WorkspaceService workspaceService,
      Provider<PodApi> podApiProvider,
      Provider<DbUser> userProvider,
      Clock clock) {

    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.userDao = userDao;
    this.folderSyncTransferDao = folderSyncTransferDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
    this.cloudStorageClient = cloudStorageClient;
    this.storageTransferClient = storageTransferClient;
    this.taskQueueService = taskQueueService;
    this.vwbUserService = vwbUserService;
    this.mailService = mailService;
    this.workspaceService = workspaceService;
    this.podApiProvider = podApiProvider;
    this.userProvider = userProvider;
    this.clock = clock;
  }

  @Override
  public void startWorkspaceMigration(
      String namespace,
      String terraName,
      List<String> folders,
      String podId,
      String researchPurpose) {

    Duration bucketDelay = Duration.ofSeconds(10);
    DbWorkspace dbWorkspace = workspaceDao.getRequired(namespace, terraName);

    dbWorkspace.setMigrationState(MigrationState.STARTING.name());
    dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    workspaceDao.save(dbWorkspace);
    try {
      RawlsWorkspaceDetails fcWorkspace =
          fireCloudService.getWorkspace(namespace, terraName).getWorkspace();

      Workspace workspace =
          workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace, initialCreditsService);

      String resolvedPodId =
          podId != null
              ? podId
              : Optional.ofNullable(userDao.findUserByUsername(workspace.getCreator()))
                  .map(DbUser::getVwbUserPod)
                  .map(DbVwbUserPod::getVwbPodId)
                  .orElse(workbenchConfigProvider.get().vwb.defaultPodId);
      WorkspaceDescription vwbWorkspace;
      if (workspace.getMigratedVwbWorkspaceId() == null) {
        try {
          logger.log(Level.INFO, namespace + ": Starting workspace creation");

          vwbWorkspace = wsmClient.createWorkspaceAsService(workspace, resolvedPodId);
        } catch (Exception e) {
          dbWorkspace.setMigrationState(MigrationState.NOT_STARTED.name());
          dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
          workspaceDao.save(dbWorkspace);
          throw new RuntimeException(namespace + ": Workspace creation failed", e);
        }
      } else {
        try {
          logger.log(
              Level.INFO,
              namespace
                  + ": Fetching existing workspace with UUID "
                  + workspace.getMigratedVwbWorkspaceId());

          vwbWorkspace = wsmClient.getWorkspaceAsService(namespace);
        } catch (Exception e) {
          dbWorkspace.setMigrationState(MigrationState.FAILED.name());
          dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
          workspaceDao.save(dbWorkspace);
          throw new RuntimeException(namespace + ": Workspace fetch failed", e);
        }
      }

      UUID workspaceId = vwbWorkspace.getId();
      dbWorkspace.setMigratedVwbWorkspaceId(workspaceId.toString());
      dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
      workspaceDao.save(dbWorkspace);

      String userEmail = workspace.getCreator();
      wsmClient.shareWorkspaceAsService(workspaceId.toString(), userEmail, IamRole.OWNER);

      logger.log(Level.INFO, namespace + ": Fetching existing collaborators from Terra");
      RawlsWorkspaceACL acl = fireCloudService.getWorkspaceAclAsService(namespace, terraName);

      if (acl != null && acl.getAcl() != null) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String aclJson = gson.toJson(acl.getAcl());
        Map<String, RawlsWorkspaceAccessEntry> aclMap =
            gson.fromJson(
                aclJson,
                new com.google.gson.reflect.TypeToken<
                    Map<String, RawlsWorkspaceAccessEntry>>() {}.getType());

        for (Map.Entry<String, RawlsWorkspaceAccessEntry> entry : aclMap.entrySet()) {
          String collaboratorEmail = entry.getKey();

          // Skip creator, already shared above
          if (collaboratorEmail.equals(userEmail)) {
            continue;
          }

          try {
            OrganizationMember member = vwbUserService.getOrganizationMember(collaboratorEmail);

            // Skip if not found in VWB
            if (member == null || member.getUserDescription() == null) {
              logger.log(
                  Level.INFO,
                  namespace + ": Skipping collaborator not found in VWB: " + collaboratorEmail);
              continue;
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
              continue;
            }

            // Map Terra role to VWB IamRole
            IamRole vwbRole = mapTerraRoleToVwbRole(entry.getValue().getAccessLevel());
            if (vwbRole == null) {
              logger.log(
                  Level.INFO,
                  namespace + ": Skipping collaborator with unmappable role: " + collaboratorEmail);
              continue;
            }

            logger.log(
                Level.INFO,
                namespace + ": Sharing workspace with collaborator: " + collaboratorEmail);
            wsmClient.shareWorkspaceAsService(workspaceId.toString(), collaboratorEmail, vwbRole);

          } catch (Exception e) {
            // Don't fail entire migration for one collaborator
            logger.log(
                Level.WARNING,
                namespace + ": Failed to share with collaborator: " + collaboratorEmail,
                e);
          }
        }
      }

      List<Property> properties =
          List.of(
              new Property().key("terra-default-location").value("us-central1"),
              new Property().key("terra-required-data-use-metadata").value(researchPurpose),
              new Property().key("terra-workspace-short-description").value(""));
      wsmClient.updateWorkspaceProperties(properties, workspaceId.toString());
      logger.log(Level.INFO, namespace + ": Workspace created: " + vwbWorkspace);

      long cdrVersionId = dbWorkspace.getCdrVersion().getCdrVersionId();
      CdrVersionForMigration cdrVersionForMigration =
          workbenchConfigProvider.get().vwb.cdrVersionsForMigration.stream()
              .filter(c -> c.cdrVersionId == cdrVersionId)
              .findFirst()
              .orElse(null);
      if (cdrVersionForMigration == null) {
        throw new RuntimeException(namespace + ": CDR version not available for migration");
      }

      // Attach data collection
      try {
        logger.log(Level.INFO, namespace + ": Starting BQ clone");
        wsmClient.cloneBQDataset(
            workspaceId,
            cdrVersionForMigration.workspaceId,
            UUID.fromString(cdrVersionForMigration.resourceId),
            UUID.randomUUID().toString());

        logger.log(Level.INFO, namespace + ": BQ clone complete");
      } catch (Exception e) {
        throw new RuntimeException(namespace + ": BQ clone failed", e);
      }

      CreatedControlledGcpGcsBucket controlledBucket;
      try {
        logger.log(Level.INFO, namespace + ": Creating bucket ");
        controlledBucket = wsmClient.createControlledBucket(workspaceId.toString(), namespace);
        logger.log(
            Level.INFO, namespace + ": Bucket creation complete: " + controlledBucket.toString());
      } catch (ApiException e) {
        if (e.getCode() == 409) {
          // Duplicate bucket name, modify name and try again
          String modifiedNamespace = namespace + UUID.randomUUID().toString().substring(0, 4);
          try {
            logger.log(
                Level.INFO,
                namespace + ": Creating bucket with modified namespace: " + modifiedNamespace);
            controlledBucket =
                wsmClient.createControlledBucket(workspaceId.toString(), modifiedNamespace);
            logger.log(
                Level.INFO,
                namespace + ": Modified bucket creation complete: " + controlledBucket.toString());
          } catch (ApiException ex) {
            throw new RuntimeException(namespace + ": Modified bucket creation failed", ex);
          }
        } else {
          throw new RuntimeException(namespace + ": Bucket creation failed", e);
        }
      }

      // Make sure new bucket is ready for transfer
      Thread.sleep(bucketDelay.toMillis());

      WorkspaceDescription vwbWorkspaceWithPolicies =
          wsmClient.getWorkspaceAsService(vwbWorkspace.getUserFacingId());
      logger.log(
          Level.INFO, namespace + ": New workspace with policies: " + vwbWorkspaceWithPolicies);

      String destinationBucket = controlledBucket.getGcpBucket().getAttributes().getBucketName();

      String sourceBucket = fcWorkspace.getBucketName();

      String serviceAccountEmail = workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0);

      String projectId = workbenchConfigProvider.get().server.projectId;
      logger.log(Level.INFO, namespace + ": Creating transfer job");

      String jobName =
          storageTransferClient.createTransferJob(
              sourceBucket,
              destinationBucket,
              workspace.getNamespace(),
              projectId,
              folders,
              serviceAccountEmail,
              false);
      logger.log(Level.INFO, namespace + ": Job created, running transfer job");

      storageTransferClient.runTransferJob(projectId, jobName);
      logger.log(Level.INFO, namespace + ": Running transfer queue");

      taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);

    } catch (Exception e) {
      dbWorkspace.setMigrationState(MigrationState.FAILED.name());
      dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
      workspaceDao.save(dbWorkspace);

      throw new RuntimeException(namespace + ": Workspace migration failed to start", e);
    }
  }

  @Override
  public void syncWorkspaceFolders(String namespace, String terraName, List<String> folders) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(namespace, terraName);
    RawlsWorkspaceDetails fcWorkspace =
        fireCloudService.getWorkspace(namespace, terraName).getWorkspace();
    WorkspaceDescription vwbWorkspace;
    try {
      logger.log(
          Level.INFO,
          namespace
              + ": Fetching 2.0 workspace with UUID "
              + dbWorkspace.getMigratedVwbWorkspaceId());

      vwbWorkspace = wsmClient.getWorkspaceAsService(namespace);
    } catch (Exception e) {
      throw new RuntimeException(namespace + ": Workspace fetch failed", e);
    }

    try {
      logger.log(
          Level.INFO,
          namespace
              + ": Fetching 2.0 workspace resources"
              + dbWorkspace.getMigratedVwbWorkspaceId());
      Object resources = wsmClient.enumerateWorkspaceResources(vwbWorkspace.getId().toString());
      String destinationBucket = "rw-migration-" + namespace;
      ResourceDescription bucket =
          ((ResourceList) resources)
              .getResources().stream()
                  .filter(
                      r ->
                          r.getMetadata().getResourceType() == ResourceType.GCS_BUCKET
                              && r.getMetadata().getName().startsWith(destinationBucket))
                  .findFirst()
                  .orElse(null);
      if (bucket == null) {
        throw new RuntimeException(namespace + ": Bucket not found");
      }
      logger.log(Level.INFO, namespace + ": Workspace bucket: " + bucket);
      logger.log(Level.INFO, namespace + ": Creating transfer job");

      String sourceBucket = fcWorkspace.getBucketName();
      String serviceAccountEmail = workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0);
      String projectId = workbenchConfigProvider.get().server.projectId;
      String jobName =
          storageTransferClient.createTransferJob(
              sourceBucket,
              bucket.getMetadata().getName(),
              namespace,
              projectId,
              folders,
              serviceAccountEmail,
              true);
      logger.log(Level.INFO, namespace + ": Job created, running transfer job");
      storageTransferClient.runTransferJob(projectId, jobName);
      DbUser user = userProvider.get();
      folderSyncTransferDao.save(
          new DbFolderSyncTransfer()
              .setCreatedByUserId(user.getUserId())
              .setStarted(new Timestamp(clock.instant().toEpochMilli()))
              .setTransferJobName(jobName)
              .setTransferState(TransferState.IN_PROGRESS.toString())
              .setSourceWorkspaceNamespace(namespace));
      logger.log(Level.INFO, namespace + ": Running transfer queue");
      taskQueueService.pushFolderSyncStatusTask(namespace, terraName, jobName);
    } catch (Exception e) {
      throw new RuntimeException(namespace + ": Failed to fetch workspace resources", e);
    }
  }

  @Override
  public MigrationBucketContentsResponse getBucketContents(String namespace, String terraName) {

    RawlsWorkspaceDetails fcWorkspace =
        fireCloudService.getWorkspace(namespace, terraName).getWorkspace();

    String bucketName = fcWorkspace.getBucketName();

    List<Blob> blobs = cloudStorageClient.getBlobPage(bucketName);

    Set<String> folderSet = new HashSet<>();

    for (Blob blob : blobs) {
      String name = blob.getName();

      if (name != null && name.contains("/")) {
        String folder = name.substring(0, name.indexOf("/") + 1);
        folderSet.add(folder);
      }
    }

    List<String> folders = new ArrayList<>(folderSet);
    Collections.sort(folders);

    return new MigrationBucketContentsResponse().bucketName(bucketName).folders(folders);
  }

  @Override
  public void checkMigrationStatus(String namespace, String terraName) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(namespace, terraName);
    String projectId = workbenchConfigProvider.get().server.projectId;
    String workspaceNamespace = dbWorkspace.getWorkspaceNamespace();
    String jobName = "transferJobs/migration-" + workspaceNamespace;
    TransferOperation transferOperation =
        storageTransferClient.getTransferJobStatus(projectId, jobName);
    TransferOperation.Status jobStatus = transferOperation.getStatus();
    logger.log(Level.INFO, "Job status: " + jobStatus);
    switch (jobStatus) {
      case IN_PROGRESS:
      case QUEUED:
        // STS still running — requeue
        taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);
        return;
      case FAILED:
        dbWorkspace.setMigrationState(MigrationState.FAILED.name());
        dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
        workspaceDao.save(dbWorkspace);
        storageTransferClient.deleteTransferJob(projectId, jobName);
        return;
      case SUCCESS:
        dbWorkspace.setMigrationState(MigrationState.FINISHED.name());
        dbWorkspace.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
        workspaceDao.save(dbWorkspace);
        try {
          List<DbUser> owners = workspaceService.getWorkspaceOwnerList(dbWorkspace);

          mailService.sendWorkspaceMigrationCompleteEmail(dbWorkspace, owners);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to send migration completion email", e);
        }
        storageTransferClient.deleteTransferJob(projectId, jobName);
    }
  }

  @Override
  public void checkFolderSyncStatus(String namespace, String terraName, String jobName) {
    DbFolderSyncTransfer dbFolderSyncTransfer =
        folderSyncTransferDao.findDbFolderSyncTransferByTransferJobName(jobName);
    String projectId = workbenchConfigProvider.get().server.projectId;
    TransferOperation transferOperation =
        storageTransferClient.getTransferJobStatus(projectId, jobName);
    TransferOperation.Status jobStatus = transferOperation.getStatus();
    logger.log(Level.INFO, "Job status: " + jobStatus);
    switch (jobStatus) {
      case IN_PROGRESS:
      case QUEUED:
        // STS still running — requeue
        taskQueueService.pushFolderSyncStatusTask(namespace, terraName, jobName);
        return;
      case FAILED:
        // storageTransferClient.deleteTransferJob(projectId, jobName);
        dbFolderSyncTransfer
            .setTransferState(TransferState.FAILED.toString())
            .setFinished(new Timestamp(clock.instant().toEpochMilli()));
        folderSyncTransferDao.save(dbFolderSyncTransfer);
        return;
      case SUCCESS:
        // storageTransferClient.deleteTransferJob(projectId, jobName);
        dbFolderSyncTransfer
            .setTransferState(TransferState.FINISHED.toString())
            .setFinished(new Timestamp(clock.instant().toEpochMilli()));
        folderSyncTransferDao.save(dbFolderSyncTransfer);
    }
  }

  @Override
  public void startPreprodWorkspaceMigration(
      PreprodWorkspace preprodWorkspace, String email, String researchPurpose, String bucketName) {

    Duration bucketDelay = Duration.ofSeconds(10);
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;

    try {

      String resolvedPodId =
          podApiProvider
              .get()
              .getPod(organizationId, "~nph-consortium-users", PodAction.READ_METADATA)
              .getPodId()
              .toString();
      logger.log(
          Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": Starting workspace creation");

      WorkspaceDescription vwbWorkspace =
          wsmClient.createWorkspaceFromPreprodAsService(preprodWorkspace, resolvedPodId);

      UUID workspaceId = vwbWorkspace.getId();

      wsmClient.shareWorkspaceAsService(workspaceId.toString(), email, IamRole.OWNER);

      List<Property> properties =
          List.of(
              new Property().key("terra-default-location").value("us-central1"),
              new Property().key("terra-required-data-use-metadata").value(researchPurpose),
              new Property().key("terra-workspace-short-description").value(""));
      wsmClient.updateWorkspaceProperties(properties, workspaceId.toString());
      logger.log(
          Level.INFO,
          preprodWorkspace.getWorkspaceNamespace() + ": Workspace created: " + vwbWorkspace);

      // Manually map preprod cdrs to data collection ids from config since preprod cdrVersionIds
      // will not match prod
      long cdrVersionId = preprodWorkspace.getCdrVersionId();
      String dataCollectionWsid;
      String dataCollectionResourceId =
          switch (Long.toString(cdrVersionId)) {
            case "10", "12" -> {
              dataCollectionWsid =
                  workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(0).workspaceId;
              yield workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(0).resourceId;
            }
            case "11", "13" -> {
              dataCollectionWsid =
                  workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(1).workspaceId;
              yield workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(1).resourceId;
            }
            case "14" -> {
              dataCollectionWsid =
                  workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(2).workspaceId;
              yield workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(2).resourceId;
            }
            case "15" -> {
              dataCollectionWsid =
                  workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(3).workspaceId;
              yield workbenchConfigProvider.get().vwb.cdrVersionsForMigration.get(3).resourceId;
            }
            default -> throw new RuntimeException(
                preprodWorkspace.getWorkspaceNamespace()
                    + ": Preprod CDR version not available for migration");
          };

      logger.log(Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": Starting BQ clone");
      wsmClient.cloneBQDataset(
          workspaceId,
          dataCollectionWsid,
          UUID.fromString(dataCollectionResourceId),
          UUID.randomUUID().toString());

      logger.log(Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": BQ clone complete");
      logger.log(Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": Creating bucket ");
      CreatedControlledGcpGcsBucket controlledBucket =
          wsmClient.createControlledBucket(
              workspaceId.toString(), preprodWorkspace.getWorkspaceNamespace());
      logger.log(
          Level.INFO,
          preprodWorkspace.getWorkspaceNamespace()
              + ": Bucket creation complete: "
              + controlledBucket.toString());

      // Make sure new bucket is ready for transfer
      Thread.sleep(bucketDelay.toMillis());

      WorkspaceDescription vwbWorkspaceWithPolicies =
          wsmClient.getWorkspaceAsService(vwbWorkspace.getUserFacingId());
      logger.log(
          Level.INFO,
          preprodWorkspace.getWorkspaceNamespace()
              + ": New workspace with policies: "
              + vwbWorkspaceWithPolicies);

      String destinationBucket = controlledBucket.getGcpBucket().getAttributes().getBucketName();

      String serviceAccountEmail = workbenchConfigProvider.get().auth.serviceAccountApiUsers.get(0);

      String projectId = workbenchConfigProvider.get().server.projectId;
      logger.log(Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": Creating transfer job");

      String jobName =
          storageTransferClient.createTransferJob(
              bucketName,
              destinationBucket,
              preprodWorkspace.getWorkspaceNamespace(),
              projectId,
              null,
              serviceAccountEmail,
              true);
      logger.log(
          Level.INFO,
          preprodWorkspace.getWorkspaceNamespace() + ": Job created, running transfer job");

      storageTransferClient.runTransferJob(projectId, jobName);
      logger.log(Level.INFO, preprodWorkspace.getWorkspaceNamespace() + ": Running transfer queue");
    } catch (Exception e) {
      throw new RuntimeException(
          preprodWorkspace.getWorkspaceNamespace() + ": Workspace migration failed to start", e);
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
