package org.pmiops.workbench.cloudtasks;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.springframework.stereotype.Service;

@Service
public class TaskQueueService {
  private static final String BASE_PATH = "/v1/cloudTask";
  private static final String EXPORT_RESEARCHER_PATH = BASE_PATH + "/exportResearcherData";
  private static final String EXPORT_WORKSPACE_PATH = BASE_PATH + "/exportWorkspaceData";
  private static final String AUDIT_PROJECTS_PATH = BASE_PATH + "/auditProjectAccess";

  private static final String AUDIT_PROJECTS_QUEUE_NAME = "auditProjectQueue";

  private WorkbenchLocationConfigService locationConfigService;
  private Provider<CloudTasksClient> cloudTasksClientProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  public TaskQueueService(
      WorkbenchLocationConfigService locationConfigService,
      Provider<CloudTasksClient> cloudTasksClientProvider,
      Provider<WorkbenchConfig> configProvider) {
    this.locationConfigService = locationConfigService;
    this.cloudTasksClientProvider = cloudTasksClientProvider;
    this.workbenchConfigProvider = configProvider;
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

  private void createAndPushTask(String queueName, String taskUri, Object jsonBody) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    String queuePath =
        QueueName.of(
                workbenchConfig.server.projectId,
                locationConfigService.getCloudTaskLocationId(),
                queueName)
            .toString();
    String body = new Gson().toJson(jsonBody);
    cloudTasksClientProvider
        .get()
        .createTask(
            queuePath,
            Task.newBuilder()
                .setAppEngineHttpRequest(
                    AppEngineHttpRequest.newBuilder()
                        .setRelativeUri(taskUri)
                        .setBody(ByteString.copyFromUtf8(body))
                        .setHttpMethod(HttpMethod.POST)
                        .putHeaders("Content-type", "application/json"))
                .build());
  }
}
