package org.pmiops.workbench.cloudtasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.pmiops.workbench.model.CreateWorkspaceTaskRequest;
import org.pmiops.workbench.model.DuplicateWorkspaceTaskRequest;
import org.pmiops.workbench.model.ProcessEgressEventRequest;
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

  private static final String AUDIT_PROJECTS_QUEUE_NAME = "auditProjectQueue";
  private static final String SYNCHRONIZE_ACCESS_QUEUE_NAME = "synchronizeAccessQueue";
  private static final String EGRESS_EVENT_QUEUE_NAME = "egressEventQueue";
  private static final String CREATE_WORKSPACE_QUEUE_NAME = "createWorkspaceQueue";
  private static final String DUPLICATE_WORKSPACE_QUEUE_NAME = "duplicateWorkspaceQueue";

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
    groupAndPushRdrWorkspaceTasks(workspaceIds, false);
  }

  public void groupAndPushRdrWorkspaceTasks(List<Long> workspaceIds, boolean backfill) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    List<List<Long>> groups =
        CloudTasksUtils.partitionList(workspaceIds, workbenchConfig.rdrExport.exportObjectsPerTask);
    String path = EXPORT_WORKSPACE_PATH;
    if (backfill) {
      path += "?backfill=true";
    }
    for (List<Long> group : groups) {
      createAndPushTask(workbenchConfig.rdrExport.queueName, path, group);
    }
  }

  public void groupAndPushRdrResearcherTasks(List<Long> userIds) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    List<List<Long>> groups =
        CloudTasksUtils.partitionList(userIds, workbenchConfig.rdrExport.exportObjectsPerTask);
    for (List<Long> group : groups) {
      createAndPushTask(workbenchConfig.rdrExport.queueName, EXPORT_RESEARCHER_PATH, group);
    }
  }

  public void groupAndPushAuditProjectsTasks(List<Long> userIds) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    List<List<Long>> groups =
        CloudTasksUtils.partitionList(userIds, workbenchConfig.offlineBatch.usersPerAuditTask);
    for (List<Long> group : groups) {
      createAndPushTask(
          AUDIT_PROJECTS_QUEUE_NAME,
          AUDIT_PROJECTS_PATH,
          new AuditProjectAccessRequest().userIds(group));
    }
  }

  public List<String> groupAndPushSynchronizeAccessTasks(List<Long> userIds) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    List<List<Long>> groups =
        CloudTasksUtils.partitionList(
            userIds, workbenchConfig.offlineBatch.usersPerSynchronizeAccessTask);
    List<String> tasknames = new ArrayList<>();
    for (List<Long> group : groups) {
      tasknames.add(
          createAndPushTask(
              SYNCHRONIZE_ACCESS_QUEUE_NAME,
              SYNCHRONIZE_ACCESS_PATH,
              new AuditProjectAccessRequest().userIds(group)));
    }
    return tasknames;
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
