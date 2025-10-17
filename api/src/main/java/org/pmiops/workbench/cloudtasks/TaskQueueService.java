package org.pmiops.workbench.cloudtasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.OfflineBatchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.RdrExportConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.*;
import org.springframework.stereotype.Service;

@Service
public class TaskQueueService {

  // when adding or updating a queue, it's important to also update:
  // - queue.yaml (the deployment process uses this file to actually create the queue)
  // - the alerting policies in the workbench-terraform-modules repo, at:
  // /modules/workbench/modules/monitoring/modules/alert_policies/assets/alert_policies
  public static final TaskQueuePair ACCESS_EXPIRATION_EMAIL =
      new TaskQueuePair("accessExpirationEmailQueue", "sendAccessExpirationEmails");
  public static final TaskQueuePair AUDIT_PROJECTS =
      new TaskQueuePair("auditProjectQueue", "auditProjectAccess");
  public static final TaskQueuePair CREATE_WORKSPACE =
      new TaskQueuePair("createWorkspaceQueue", "createWorkspace");
  public static final TaskQueuePair DELETE_RAWLS_TEST_WORKSPACES =
      new TaskQueuePair("deleteTestUserRawlsWorkspacesQueue", "deleteTestUserWorkspacesInRawls");
  public static final TaskQueuePair DELETE_TEST_WORKSPACES =
      new TaskQueuePair("deleteTestUserWorkspacesQueue", "deleteTestUserWorkspaces");
  public static final TaskQueuePair DELETE_UNSHARED_WORKSPACE_ENVIRONMENTS =
      new TaskQueuePair(
          "deleteUnsharedWorkspaceEnvironmentsQueue", "deleteUnsharedWorkspaceEnvironments");
  public static final TaskQueuePair DUPLICATE_WORKSPACE =
      new TaskQueuePair("duplicateWorkspaceQueue", "duplicateWorkspace");
  public static final TaskQueuePair EGRESS_EVENT =
      new TaskQueuePair("egressEventQueue", "processEgressEvent");
  public static final TaskQueuePair PERSISTENT_DISKS =
      new TaskQueuePair("checkPersistentDiskQueue", "checkPersistentDisks");
  public static final TaskQueuePair SYNCHRONIZE_ACCESS =
      new TaskQueuePair("synchronizeAccessQueue", "synchronizeUserAccess");
  public static final TaskQueuePair CLEANUP_ORPHANED_WORKSPACES =
      new TaskQueuePair("cleanupOrphanedWorkspacesQueue", "cleanupOrphanedWorkspaces");

  // RDR exporting uniquely uses the same queue for two endpoints

  public static final String RDR_EXPORT_QUEUE_NAME = "rdrExportQueue";
  public static final TaskQueuePair RDR_RESEARCHER =
      new TaskQueuePair(RDR_EXPORT_QUEUE_NAME, "exportResearcherData");
  public static final TaskQueuePair RDR_WORKSPACE =
      new TaskQueuePair(RDR_EXPORT_QUEUE_NAME, "exportWorkspaceData");

  // initial credits queues and cloud tasks:
  //
  // INITIAL_CREDITS_EXPIRATION - run as part of the cron checkInitialCreditsExpiration to check
  // whether users have passed their initial credits expiration time and execute the consequences of
  // user initial credit expiration (DB update, deletion of runtimes)
  //
  // INITIAL_CREDITS_USAGE - run as part of the cron checkInitialCreditsUsage after retrieving
  // per-workspace usage, to check all users' initial credits usage. All credit-exhausted users are
  // then pushed to the INITIAL_CREDITS_EXHAUSTION task queue.
  //
  // INITIAL_CREDITS_EXHAUSTION - run as a sub-task of checkInitialCreditsUsage, this executes the
  // consequences of user initial credit exhaustion (DB update, deletion of runtimes, sending email)

  public static final TaskQueuePair INITIAL_CREDITS_EXPIRATION =
      new TaskQueuePair(
          "checkCreditsExpirationForUserIDsQueue", "checkCreditsExpirationForUserIDs");
  public static final TaskQueuePair INITIAL_CREDITS_USAGE =
      new TaskQueuePair("initialCreditsUsageQueue", "checkInitialCreditsUsage");
  public static final TaskQueuePair INITIAL_CREDITS_EXHAUSTION =
      new TaskQueuePair("initialCreditsExhaustionQueue", "handleInitialCreditsExhaustion");

  // VWB Pod Creation
  public static final TaskQueuePair VWB_POD_CREATION =
      new TaskQueuePair("vwbPodCreationQueue", "createVwbPod");

  public static final TaskQueuePair REPORTING_UPLOAD =
      new TaskQueuePair("reportingUploadQueue", "reportingUploadQueue");

  public static final TaskQueuePair CACHE_WORKSPACE_USERS =
      new TaskQueuePair("workspaceUserCacheQueue", "updateWorkspaceUserCache");

  private static final Logger LOGGER = Logger.getLogger(TaskQueueService.class.getName());

  private final WorkbenchLocationConfigService locationConfigService;
  private final Provider<CloudTasksClient> cloudTasksClientProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<UserAuthentication> userAuthenticationProvider;

  private final Gson gson;

  // by default, enums are serialized as the string representation of their enum constants
  // (e.g. "SSD" for DiskType.SSD) but we need to use their values ("pd-ssd") instead.
  private static Gson intializeGson() {
    return new Gson()
        .newBuilder()
        .registerTypeAdapter(DiskStatus.class, new EnumSerializer<>())
        .registerTypeAdapter(DiskType.class, new EnumSerializer<>())
        .create();
  }

  public TaskQueueService(
      WorkbenchLocationConfigService locationConfigService,
      Provider<CloudTasksClient> cloudTasksClientProvider,
      Provider<WorkbenchConfig> configProvider,
      Provider<UserAuthentication> userAuthenticationProvider) {
    this.locationConfigService = locationConfigService;
    this.cloudTasksClientProvider = cloudTasksClientProvider;
    this.workbenchConfigProvider = configProvider;
    this.userAuthenticationProvider = userAuthenticationProvider;
    this.gson = intializeGson();
  }

  public void groupAndPushRdrWorkspaceTasks(List<Long> workspaceIds) {
    groupAndPushRdrTasks(workspaceIds, RDR_WORKSPACE);
  }

  public void groupAndPushRdrWorkspaceBackfillTasks(List<Long> workspaceIds) {
    groupAndPushRdrTasks(workspaceIds, withRdrBackfill(RDR_WORKSPACE));
  }

  public void groupAndPushRdrResearcherTasks(List<Long> userIds) {
    groupAndPushRdrTasks(userIds, RDR_RESEARCHER);
  }

  public void groupAndPushRdrResearcherBackfillTasks(List<Long> userIds) {
    groupAndPushRdrTasks(userIds, withRdrBackfill(RDR_RESEARCHER));
  }

  public void groupAndPushRdrTasks(List<Long> ids, TaskQueuePair pair) {
    RdrExportConfig rdrConfig = workbenchConfigProvider.get().rdrExport;
    if (rdrConfig == null) {
      LOGGER.info("RDR export is not configured for this environment.  Exiting.");
      return;
    }

    createAndPushAll(ids, rdrConfig.exportObjectsPerTask, pair);
  }

  public void groupAndPushAuditProjectsTasks(List<Long> userIds) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(userIds, config.usersPerAuditTask, AUDIT_PROJECTS);
  }

  public void groupAndPushInitialCreditsUsage(List<Long> userIds) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(userIds, config.usersPerCheckInitialCreditsUsageTask, INITIAL_CREDITS_USAGE);
  }

  public List<String> groupAndPushSynchronizeAccessTasks(List<Long> userIds) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    return createAndPushAll(userIds, config.usersPerSynchronizeAccessTask, SYNCHRONIZE_ACCESS);
  }

  public void groupAndPushCleanupOrphanedWorkspacesTasks(List<String> workspaceNamespaces) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(
        workspaceNamespaces,
        config.workspacesPerCleanupOrphanedWorkspacesTask,
        CLEANUP_ORPHANED_WORKSPACES);
  }

  public void groupAndPushAccessExpirationEmailTasks(List<Long> userIds) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(userIds, config.usersPerAccessExpirationEmailTask, ACCESS_EXPIRATION_EMAIL);
  }

  private int getDeleteWorkspacesBatchSize() throws BadRequestException {
    return Optional.ofNullable(workbenchConfigProvider.get().e2eTestUsers)
        .map(conf -> conf.workspaceDeletionBatchSize)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "Deletion of e2e test user workspaces is not enabled in this environment"));
  }

  public void groupAndPushDeleteTestWorkspaceTasks(List<TestUserWorkspace> workspacesToDelete) {
    int batchSize = getDeleteWorkspacesBatchSize();
    createAndPushAll(workspacesToDelete, batchSize, DELETE_TEST_WORKSPACES);
  }

  public void groupAndPushDeleteTestWorkspaceInRawlsTasks(
      List<TestUserRawlsWorkspace> workspacesToDelete) {
    int batchSize = getDeleteWorkspacesBatchSize();
    createAndPushAll(workspacesToDelete, batchSize, DELETE_RAWLS_TEST_WORKSPACES);
  }

  public void groupAndPushDeleteWorkspaceEnvironmentTasks(List<String> workspaceNamespaces) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(
        workspaceNamespaces,
        config.workspacesPerDeleteWorkspaceEnvironmentsTask,
        DELETE_UNSHARED_WORKSPACE_ENVIRONMENTS);
  }

  public void pushEgressEventTask(Long eventId, boolean isVwbEgressEvent) {
    createAndPushTask(
        EGRESS_EVENT,
        new ProcessEgressEventRequest().eventId(eventId).isVwbEgressEvent(isVwbEgressEvent));
  }

  public void pushCreateWorkspaceTask(long operationId, Workspace workspace) {
    createAndPushTaskWithBearerToken(
        CREATE_WORKSPACE,
        new CreateWorkspaceTaskRequest().operationId(operationId).workspace(workspace));
  }

  public void pushDuplicateWorkspaceTask(
      long operationId,
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      Boolean shouldDuplicateRoles,
      Workspace workspace) {
    createAndPushTaskWithBearerToken(
        DUPLICATE_WORKSPACE,
        new DuplicateWorkspaceTaskRequest()
            .operationId(operationId)
            .fromWorkspaceNamespace(fromWorkspaceNamespace)
            .fromWorkspaceFirecloudName(fromWorkspaceFirecloudName)
            .shouldDuplicateRoles(shouldDuplicateRoles)
            .workspace(workspace));
  }

  public void pushInitialCreditsExhaustionTask(
      List<Long> users, Map<Long, Double> dbCostByCreator, Map<Long, Double> liveCostByCreator) {
    createAndPushTask(
        INITIAL_CREDITS_EXHAUSTION,
        new ExhaustedInitialCreditsEventRequest()
            .users(users)
            .dbCostByCreator(dbCostByCreator)
            .liveCostByCreator(liveCostByCreator));
  }

  public List<String> groupAndPushCheckInitialCreditsExpirationTasks(List<Long> userIds) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    return createAndPushAll(
        userIds, config.usersPerCheckInitialCreditsExpirationTask, INITIAL_CREDITS_EXPIRATION);
  }

  public void groupAndPushCheckPersistentDiskTasks(List<Disk> disks) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    createAndPushAll(disks, config.disksPerCheckPersistentDiskTask, PERSISTENT_DISKS);
  }

  public void pushVwbPodCreationTask(String email) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserCreation
        || !workbenchConfigProvider.get().featureFlags.enableVWBPodCreation) {
      return;
    }
    createAndPushTaskWithBearerToken(
        VWB_POD_CREATION, new CreateVwbPodTaskRequest().userName(email));
  }

  public void pushReportingUploadTask(String table, Long captureSnapshotTime) {
    createAndPushTask(
        REPORTING_UPLOAD,
        new ReportingUploadQueueTaskRequest()
            .tables(List.of(table))
            .snapshotTimestamp(captureSnapshotTime));
  }

  public void pushWorkspaceUserCacheTask(List<WorkspaceDao.WorkspaceUserCacheView> workspaces) {
    OfflineBatchConfig config = workbenchConfigProvider.get().offlineBatch;
    var request =
        workspaces.stream()
            .map(
                workspace ->
                    new WorkspaceUserCacheQueueWorkspace()
                        .workspaceId(workspace.getWorkspaceId())
                        .workspaceNamespace(workspace.getWorkspaceNamespace())
                        .workspaceFirecloudName(workspace.getFirecloudName()))
            .toList();
    LOGGER.info("Pushing " + request.size() + " tasks onto workspace user cache queue");
    createAndPushAll(request, config.workspacesPerWorkspaceUserCacheTask, CACHE_WORKSPACE_USERS);
  }

  private TaskQueuePair withRdrBackfill(TaskQueuePair pair) {
    return new TaskQueuePair(pair.queueName(), pair.endpoint() + "?backfill=true");
  }

  private <T> List<String> createAndPushAll(List<List<T>> batches, TaskQueuePair pair) {
    LOGGER.info("Pushing " + batches.size() + " tasks onto " + pair.queueName());
    return batches.stream()
        .map(
            batch -> {
              LOGGER.info("Task contains " + batch.size() + " items");
              return createAndPushTask(pair, batch);
            })
        .toList();
  }

  private <T> List<String> createAndPushAll(List<T> fullList, int batchSize, TaskQueuePair pair) {
    return createAndPushAll(CloudTasksUtils.partitionList(fullList, batchSize), pair);
  }

  private String createAndPushTask(TaskQueuePair pair, Object jsonBody) {
    return createAndPushTask(pair, jsonBody, Map.of());
  }

  private String createAndPushTaskWithBearerToken(TaskQueuePair pair, Object jsonBody) {
    return createAndPushTask(
        pair,
        jsonBody,
        Map.of("Authorization", "Bearer " + userAuthenticationProvider.get().getCredentials()));
  }

  private String createAndPushTask(
      TaskQueuePair pair, Object jsonBody, Map<String, String> extraHeaders) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    String queuePath =
        QueueName.of(
                workbenchConfig.server.projectId,
                locationConfigService.getCloudTaskLocationId(),
                pair.queueName())
            .toString();
    String body = gson.toJson(jsonBody);
    return cloudTasksClientProvider
        .get()
        .createTask(
            queuePath,
            Task.newBuilder()
                .setAppEngineHttpRequest(
                    AppEngineHttpRequest.newBuilder()
                        .setRelativeUri(pair.fullPath())
                        .setBody(ByteString.copyFromUtf8(body))
                        .setHttpMethod(HttpMethod.POST)
                        .putHeaders("Content-type", "application/json")
                        .putAllHeaders(extraHeaders))
                .build())
        .getName();
  }
}
