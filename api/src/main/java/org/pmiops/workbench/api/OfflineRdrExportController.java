package org.pmiops.workbench.api;

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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.rdr.RdrExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * This offline process is responsible to daily sync up with RDR by creating/pushing multiple task
 * with eligible user Ids and workspace Ids to cloud task queue
 *
 * @author nsaxena
 */
@RestController
public class OfflineRdrExportController implements OfflineRdrExportApiDelegate {

  private static final Logger log = Logger.getLogger(OfflineRdrExportController.class.getName());
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private RdrExportService rdrExportService;
  private WorkbenchLocationConfigService locationConfigService;

  private final String BASE_PATH = "/v1/cloudTask";
  private final String EXPORT_RESEARCHER_PATH = BASE_PATH + "/exportResearcherData";
  private final String EXPORT_USER_PATH = BASE_PATH + "/exportWorkspaceData";
  private final String IDS_STRING_SPLIT = ", ";

  OfflineRdrExportController(
      WorkbenchLocationConfigService locationConfigService,
      RdrExportService rdrExportService,
      Provider<WorkbenchConfig> configProvider) {
    this.locationConfigService = locationConfigService;
    this.rdrExportService = rdrExportService;
    this.workbenchConfigProvider = configProvider;
  }

  /**
   * Purpose of this method will be to create multiple task of 2 categories, user (researcher) and
   * workspace.
   *
   * @return
   */
  @Override
  public ResponseEntity<Void> exportData() {
    // Its important to send all researcher information first to RDR before sending workspace since
    // workspace object will contain collaborator information (userId)
    if (workbenchConfigProvider.get().featureFlags.enableRdrExport) {
      try {
        groupIdsAndPushTask(rdrExportService.findAllUserIdsToExport(), EXPORT_RESEARCHER_PATH);
      } catch (Exception ex) {
        log.severe(
            String.format("Error while exporting researcher data to RDR: %s", ex.getMessage()));
      }
      try {
        groupIdsAndPushTask(rdrExportService.findAllWorkspacesIdsToExport(), EXPORT_USER_PATH);
      } catch (Exception ex) {
        log.severe(
            String.format("Error while exporting workspace data to RDR: %s", ex.getMessage()));
      }
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * For the list of Ids passed Create task with n ids (where n is configured in config files) and
   * Push them in queue
   *
   * @param idList : Lis of Ids
   * @param taskUri: The destination URL the task will be calling with group of 10 ids
   */
  private void groupIdsAndPushTask(List<Long> idList, String taskUri) {
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

  private void createAndPushTask(List<Long> ids, String queuePath, String taskUri) {
    List<String> idsAsString = ids.stream().map(id -> id.toString()).collect(Collectors.toList());
    Gson gson = new Gson();
    String daysJson = gson.toJson(ids);
    try (CloudTasksClient client = CloudTasksClient.create()) {
      String commaSepList = String.join(IDS_STRING_SPLIT, idsAsString);
      AppEngineHttpRequest req =
          AppEngineHttpRequest.newBuilder()
              .setRelativeUri(taskUri)
              .setBody(ByteString.copyFromUtf8(daysJson))
              .setHttpMethod(HttpMethod.POST)
              .putHeaders("Content-type", "application/json")
              .build();

      Task taskBuilder = Task.newBuilder().setAppEngineHttpRequest(req).build();
      Task task = client.createTask(queuePath, taskBuilder);

    } catch (IOException ex) {
      log.severe(
          String.format(
              "Error while creating task to push to queue for IDS %s and path %s",
              idsAsString, taskUri));
    }
  }
}
