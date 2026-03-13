package org.pmiops.workbench.workspaces.migration;

import com.google.cloud.storage.Blob;
import com.google.storagetransfer.v1.proto.TransferTypes;
import jakarta.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.pmiops.workbench.model.MigrationBucketContentsResponse;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMigrationServiceImpl implements WorkspaceMigrationService {

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
      TaskQueueService taskQueueService) {

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
  }

  @Override
  public void startWorkspaceMigration(String namespace, String terraName, List<String> folders) {

    DbWorkspace dbWorkspace = workspaceDao.getRequired(namespace, terraName);

    dbWorkspace.setMigrationState(MigrationState.STARTING.name());
    workspaceDao.save(dbWorkspace);

    RawlsWorkspaceDetails fcWorkspace =
        fireCloudService.getWorkspace(namespace, terraName).getWorkspace();

    Workspace workspace =
        workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace, initialCreditsService);

    // Determine pod ID using existing project pattern
    String podId =
        Optional.ofNullable(userDao.findUserByUsername(workspace.getCreator()))
            .map(DbUser::getVwbUserPod)
            .map(DbVwbUserPod::getVwbPodId)
            .orElse(workbenchConfigProvider.get().vwb.defaultPodId);

    WorkspaceDescription vwbWorkspace = wsmClient.createWorkspaceAsService(workspace, podId);
    String workspaceId = vwbWorkspace.getId().toString();
    String destinationBucket = wsmClient.createControlledBucket(workspaceId, namespace);
    String sourceBucket = fcWorkspace.getBucketName();

    storageTransferClient.startBucketTransfer(
        sourceBucket, destinationBucket, workbenchConfigProvider.get().server.projectId, folders);

    taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);
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

    TransferTypes.TransferJob job = storageTransferClient.getTransferJob(projectId);

    if (job.getStatus() == TransferTypes.TransferJob.Status.ENABLED) {
      // STS still running — requeue
      taskQueueService.pushWorkspaceMigrationStatusTask(namespace, terraName);
      return;
    }

    dbWorkspace.setMigrationState(MigrationState.FINISHED.name());
    workspaceDao.save(dbWorkspace);
  }
}
