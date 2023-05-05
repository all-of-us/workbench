package org.pmiops.workbench.cloudtasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.RdrExportConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.pmiops.workbench.model.CreateWorkspaceTaskRequest;
import org.pmiops.workbench.model.DuplicateWorkspaceTaskRequest;
import org.pmiops.workbench.model.ProcessEgressEventRequest;
import org.pmiops.workbench.model.SynchronizeUserAccessRequest;
import org.pmiops.workbench.model.TestUserRawlsWorkspace;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.pmiops.workbench.model.Workspace;
import org.springframework.stereotype.Service;

@Service
public class TaskQueueService {
  private static final String BASE_PATH = "/v1/cloudTask";
  private static final String EXPORT_RESEARCHER_PATH = BASE_PATH + "/exportResearcherData";
  private static final String EXPORT_WORKSPACE_PATH = BASE_PATH + "/exportWorkspaceData";
  private static final String AUDIT_PROJECTS_PATH = BASE_PATH + "/auditProjectAccess";
  private static final String SYNCHRONIZE_ACCESS_PATH = BASE_PATH + "/synchronizeUserAccess";
  private static final String EGRESS_EVENT_PATH = BASE_PATH + "/processEgressEvent";
  private static final String CREATE_WORKSPACE_PATH = BASE_PATH + "/createWorkspace";
  private static final String DUPLICATE_WORKSPACE_PATH = BASE_PATH + "/duplicateWorkspace";
  private static final String DELETE_TEST_WORKSPACES_PATH = BASE_PATH + "/deleteTestUserWorkspaces";
  private static final String DELETE_RAWLS_TEST_WORKSPACES_PATH =
      BASE_PATH + "/deleteTestUserWorkspacesInRawls";

  private static final String AUDIT_PROJECTS_QUEUE_NAME = "auditProjectQueue";
  private static final String SYNCHRONIZE_ACCESS_QUEUE_NAME = "synchronizeAccessQueue";
  private static final String EGRESS_EVENT_QUEUE_NAME = "egressEventQueue";
  private static final String CREATE_WORKSPACE_QUEUE_NAME = "createWorkspaceQueue";
  private static final String DUPLICATE_WORKSPACE_QUEUE_NAME = "duplicateWorkspaceQueue";
  private static final String DELETE_TEST_WORKSPACES_QUEUE_NAME = "deleteTestUserWorkspacesQueue";
  private static final String DELETE_RAWLS_TEST_WORKSPACES_QUEUE_NAME =
      "deleteTestUserRawlsWorkspacesQueue";

  private static final Logger LOGGER = Logger.getLogger(TaskQueueService.class.getName());

  private WorkbenchLocationConfigService locationConfigService;
  private Provider<CloudTasksClient> cloudTasksClientProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private Provider<UserAuthentication> userAuthenticationProvider;

  public TaskQueueService(
      WorkbenchLocationConfigService locationConfigService,
      Provider<CloudTasksClient> cloudTasksClientProvider,
      Provider<WorkbenchConfig> configProvider,
      Provider<UserAuthentication> userAuthenticationProvider) {
    this.locationConfigService = locationConfigService;
    this.cloudTasksClientProvider = cloudTasksClientProvider;
    this.workbenchConfigProvider = configProvider;
    this.userAuthenticationProvider = userAuthenticationProvider;
  }

  public void groupAndPushRdrWorkspaceTasks(List<Long> workspaceIds) {
    groupAndPushRdrTasks(workspaceIds, EXPORT_WORKSPACE_PATH, false);
  }

  public void groupAndPushRdrWorkspaceTasks(List<Long> workspaceIds, boolean backfill) {
    groupAndPushRdrTasks(workspaceIds, EXPORT_WORKSPACE_PATH, backfill);
  }

  public void groupAndPushRdrResearcherTasks(List<Long> userIds) {
    groupAndPushRdrTasks(userIds, EXPORT_RESEARCHER_PATH, false);
  }

  public void groupAndPushRdrResearcherTasks(List<Long> userIds, boolean backfill) {
    groupAndPushRdrTasks(userIds, EXPORT_RESEARCHER_PATH, backfill);
  }

  public void groupAndPushRdrTasks(List<Long> ids, String pathBase, boolean backfill) {
    RdrExportConfig rdrConfig = workbenchConfigProvider.get().rdrExport;
    if (rdrConfig == null) {
      LOGGER.info("RDR export is not configured for this environment.  Exiting.");
      return;
    }

    String path = backfill ? pathBase + "?backfill=true" : pathBase;

    CloudTasksUtils.partitionList(ids, rdrConfig.exportObjectsPerTask)
        .forEach(batch -> createAndPushTask(rdrConfig.queueName, path, batch));
  }

  public void groupAndPushAuditProjectsTasks(List<Long> userIds) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    CloudTasksUtils.partitionList(userIds, workbenchConfig.offlineBatch.usersPerAuditTask)
        .forEach(
            batch ->
                createAndPushTask(
                    AUDIT_PROJECTS_QUEUE_NAME,
                    AUDIT_PROJECTS_PATH,
                    new AuditProjectAccessRequest().userIds(batch)));
  }

  public List<String> groupAndPushSynchronizeAccessTasks(List<Long> userIds) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    return CloudTasksUtils.partitionList(
            userIds, workbenchConfig.offlineBatch.usersPerSynchronizeAccessTask)
        .stream()
        .map(
            batch ->
                createAndPushTask(
                    SYNCHRONIZE_ACCESS_QUEUE_NAME,
                    SYNCHRONIZE_ACCESS_PATH,
                    new SynchronizeUserAccessRequest().userIds(batch)))
        .collect(Collectors.toList());
  }

  public void groupAndPushDeleteTestWorkspaceTasks(List<TestUserWorkspace> workspacesToDelete) {
    groupDeleteWorkspaceTasks(workspacesToDelete)
        .forEach(
            batch ->
                createAndPushTask(
                    DELETE_TEST_WORKSPACES_QUEUE_NAME, DELETE_TEST_WORKSPACES_PATH, batch));
  }

  public void groupAndPushDeleteTestWorkspaceInRawlsTasks(
      List<TestUserRawlsWorkspace> workspacesToDelete) {
    groupDeleteWorkspaceTasks(workspacesToDelete)
        .forEach(
            batch ->
                createAndPushTask(
                    DELETE_RAWLS_TEST_WORKSPACES_QUEUE_NAME,
                    DELETE_RAWLS_TEST_WORKSPACES_PATH,
                    batch));
  }

  private <T> List<List<T>> groupDeleteWorkspaceTasks(List<T> workspacesToDelete) {
    int batchSize =
        Optional.ofNullable(workbenchConfigProvider.get().e2eTestUsers)
            .map(conf -> conf.workspaceDeletionBatchSize)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Deletion of e2e test user workspaces is not enabled in this environment"));
    return CloudTasksUtils.partitionList(workspacesToDelete, batchSize);
  }

  public void pushEgressEventTask(Long eventId) {
    createAndPushTask(
        EGRESS_EVENT_QUEUE_NAME,
        EGRESS_EVENT_PATH,
        new ProcessEgressEventRequest().eventId(eventId));
  }

  public void pushCreateWorkspaceTask(long operationId, Workspace workspace) {
    createAndPushTask(
        CREATE_WORKSPACE_QUEUE_NAME,
        CREATE_WORKSPACE_PATH,
        new CreateWorkspaceTaskRequest().operationId(operationId).workspace(workspace),
        ImmutableMap.of(
            "Authorization", "Bearer " + userAuthenticationProvider.get().getCredentials()));
  }

  public void pushDuplicateWorkspaceTask(
      long operationId,
      String fromWorkspaceNamespace,
      String fromWorkspaceFirecloudName,
      Boolean shouldDuplicateRoles,
      Workspace workspace) {
    createAndPushTask(
        DUPLICATE_WORKSPACE_QUEUE_NAME,
        DUPLICATE_WORKSPACE_PATH,
        new DuplicateWorkspaceTaskRequest()
            .operationId(operationId)
            .fromWorkspaceNamespace(fromWorkspaceNamespace)
            .fromWorkspaceFirecloudName(fromWorkspaceFirecloudName)
            .shouldDuplicateRoles(shouldDuplicateRoles)
            .workspace(workspace),
        ImmutableMap.of(
            "Authorization", "Bearer " + userAuthenticationProvider.get().getCredentials()));
  }

  private String createAndPushTask(String queueName, String taskUri, Object jsonBody) {
    return createAndPushTask(queueName, taskUri, jsonBody, ImmutableMap.of());
  }

  private String createAndPushTask(
      String queueName, String taskUri, Object jsonBody, Map<String, String> extraHeaders) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    String queuePath =
        QueueName.of(
                workbenchConfig.server.projectId,
                locationConfigService.getCloudTaskLocationId(),
                queueName)
            .toString();
    String body = new Gson().toJson(jsonBody);
    return cloudTasksClientProvider
        .get()
        .createTask(
            queuePath,
            Task.newBuilder()
                .setAppEngineHttpRequest(
                    AppEngineHttpRequest.newBuilder()
                        .setRelativeUri(taskUri)
                        .setBody(ByteString.copyFromUtf8(body))
                        .setHttpMethod(HttpMethod.POST)
                        .putHeaders("Content-type", "application/json")
                        .putAllHeaders(extraHeaders))
                .build())
        .getName();
  }
}
