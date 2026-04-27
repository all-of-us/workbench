package org.pmiops.workbench.workspaces.migration;

import com.google.cloud.storage.Blob;
import com.google.storagetransfer.v1.proto.TransferTypes.TransferOperation;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
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

  @Autowired
  public WorkspaceMigrationServiceImpl(
      WsmClient wsmClient,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      CloudStorageClient cloudStorageClient,
      StorageTransferClient storageTransferClient,
      TaskQueueService taskQueueService,
      VwbUserService vwbUserService,
      MailService mailService,
      WorkspaceService workspaceService,
      Provider<PodApi> podApiProvider) {

    this.wsmClient = wsmClient;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.userDao = userDao;
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
          workspaceDao.save(dbWorkspace);
          throw new RuntimeException(namespace + ": Workspace fetch failed", e);
        }
      }

      UUID workspaceId = vwbWorkspace.getId();
      dbWorkspace.setMigratedVwbWorkspaceId(workspaceId.toString());
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

      String accessTier = dbWorkspace.getCdrVersion().getAccessTier().getShortName();
      String dataCollectionWsid;
      String dataCollectionResourceId;

      // Get data collection id from access tier
      if (accessTier.equals("registered")) {
        dataCollectionWsid =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.registered.workspaceId;
        dataCollectionResourceId =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.registered.resourceId;
      } else if (accessTier.equals("controlled")) {
        dataCollectionWsid =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.controlled.workspaceId;
        dataCollectionResourceId =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.controlled.resourceId;
      } else {
        throw new RuntimeException(namespace + ": Workspace migration failed, invalid access tier");
      }

      // Attach data collection
      try {
        logger.log(Level.INFO, namespace + ": Starting BQ clone");
        wsmClient.cloneBQDataset(
            workspaceId,
            dataCollectionWsid,
            UUID.fromString(dataCollectionResourceId),
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
          String modifiedNamespace = namespace + UUID.randomUUID().toString().substring(0, 3);
          try {
            logger.log(
                Level.INFO,
                namespace + ": Creating bucket with modified namespace: " + modifiedNamespace);
            controlledBucket = wsmClient.createControlledBucket(workspaceId.toString(), namespace);
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
              serviceAccountEmail);
      logger.log(Level.INFO, namespace + ": Job created, running transfer job");

      storageTransferClient.runTransferJob(projectId, jobName);
      logger.log(Level.INFO, namespace + ": Running transfer queue");

      taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);

    } catch (Exception e) {
      dbWorkspace.setMigrationState(MigrationState.FAILED.name());
      workspaceDao.save(dbWorkspace);

      throw new RuntimeException(namespace + ": Workspace migration failed to start", e);
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
    TransferOperation transferOperation =
        storageTransferClient.getTransferJobStatus(projectId, workspaceNamespace);
    TransferOperation.Status jobStatus = transferOperation.getStatus();
    logger.log(Level.INFO, "Job status: " + jobStatus.toString());
    // Commenting out deleteTransferJob calls since all STS runs aren't transferring any data
    // TODO uncomment after resolving STS issue
    // String jobName = "transferJobs/migration-" + workspaceNamespace;
    switch (jobStatus) {
      case IN_PROGRESS:
      case QUEUED:
        // STS still running — requeue
        taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);
        return;
      case FAILED:
        dbWorkspace.setMigrationState(MigrationState.FAILED.name());
        workspaceDao.save(dbWorkspace);
        // storageTransferClient.deleteTransferJob(projectId, jobName);
        return;
      case SUCCESS:
        dbWorkspace.setMigrationState(MigrationState.FINISHED.name());
        workspaceDao.save(dbWorkspace);
        try {
          List<DbUser> owners = workspaceService.getWorkspaceOwnerList(dbWorkspace);

          mailService.sendWorkspaceMigrationCompleteEmail(dbWorkspace, owners);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Failed to send migration completion email", e);
        }
        // storageTransferClient.deleteTransferJob(projectId, jobName);
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

      String accessTier = preprodWorkspace.getAccessTierShortName();
      String dataCollectionWsid;
      String dataCollectionResourceId;

      if (accessTier.equals("registered")) {
        dataCollectionWsid =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.registered.workspaceId;
        dataCollectionResourceId =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.registered.resourceId;
      } else if (accessTier.equals("controlled")) {
        dataCollectionWsid =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.controlled.workspaceId;
        dataCollectionResourceId =
            workbenchConfigProvider.get().vwb.dataCollectionsForMigration.controlled.resourceId;
      } else {
        throw new RuntimeException("Workspace migration failed, invalid access tier");
      }

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
              serviceAccountEmail);
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
