package org.pmiops.workbench.rdr;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.springframework.stereotype.Service;

@Service
public class RdrTaskQueue {
  private static final String BASE_PATH = "/v1/cloudTask";
  public static final String EXPORT_RESEARCHER_PATH = BASE_PATH + "/exportResearcherData";
  public static final String EXPORT_USER_PATH = BASE_PATH + "/exportWorkspaceData";

  private WorkbenchLocationConfigService locationConfigService;
  private Provider<CloudTasksClient> cloudTasksClientProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  public RdrTaskQueue(
      WorkbenchLocationConfigService locationConfigService,
      Provider<CloudTasksClient> cloudTasksClientProvider,
      Provider<WorkbenchConfig> configProvider) {
    this.locationConfigService = locationConfigService;
    this.cloudTasksClientProvider = cloudTasksClientProvider;
    this.workbenchConfigProvider = configProvider;
  }

  /**
   * For the list of Ids passed Create task with n ids (where n is configured in config files) and
   * Push them in queue
   *
   * @param idList : Lis of Ids
   * @param taskUri: The destination URL the task will be calling with group of 10 ids
   */
  public void groupIdsAndPushTask(List<Long> idList, String taskUri) throws IOException {
    if (idList.size() == 0) return;
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    final AtomicInteger counter = new AtomicInteger();

    final Collection<List<Long>> idGroups =
        idList.stream()
            .collect(
                Collectors.groupingBy(
                    it ->
                        counter.getAndIncrement() / workbenchConfig.rdrExport.exportObjectsPerTask))
            .values();
    Iterator<List<Long>> idIterator = idGroups.iterator();
    String queuePath =
        QueueName.of(
                workbenchConfig.server.projectId,
                locationConfigService.getCloudTaskLocationId(),
                workbenchConfig.rdrExport.queueName)
            .toString();
    while (idIterator.hasNext()) {
      createAndPushTask(idIterator.next(), queuePath, taskUri);
    }
  }

  private void createAndPushTask(List<Long> ids, String queuePath, String taskUri)
      throws IOException {
    Gson gson = new Gson();
    String jsonIds = gson.toJson(ids);
    CloudTasksClient client = cloudTasksClientProvider.get();
    client.createTask(
        queuePath,
        Task.newBuilder()
            .setAppEngineHttpRequest(
                AppEngineHttpRequest.newBuilder()
                    .setRelativeUri(taskUri)
                    .setBody(ByteString.copyFromUtf8(jsonIds))
                    .setHttpMethod(HttpMethod.POST)
                    .putHeaders("Content-type", "application/json"))
            .build());
  }
}
