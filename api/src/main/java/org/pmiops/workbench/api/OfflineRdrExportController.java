package org.pmiops.workbench.api;

import com.google.cloud.tasks.v2.AppEngineHttpRequest;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.rdr.RdrExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * This offline process is responsible to daily sync up with RDR by sending all the created/modified
 * User and workspace information.
 *
 * @author nsaxena
 */
@RestController
public class OfflineRdrExportController implements OfflineRdrExportApiDelegate {

  private static final Logger log = Logger.getLogger(OfflineRdrExportController.class.getName());
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private RdrExportService rdrExportService;

  private final String BASE_PATH = "/v1/offline";
  private final String EXPORT_RESEARCHER_PATH = BASE_PATH + "/exportResearcherData";
  private final String EXPORT_USER_PATH = BASE_PATH + "/exportWorkspaceData";

  private final String IDS_STRING_SPLIT = ", ";

  OfflineRdrExportController(
      RdrExportService rdrExportService, Provider<WorkbenchConfig> configProvider) {
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
                workbenchConfig.server.location,
                workbenchConfig.rdrExport.queueName)
            .toString();
    while (idIterator.hasNext()) {
      createAndPushTask(idIterator.next(), queuePath, taskUri);
    }
  }

  private void createAndPushTask(List<Long> ids, String queuePath, String taskUri) {
    List<String> idsAsString = ids.stream().map(id -> id.toString()).collect(Collectors.toList());
    try (CloudTasksClient client = CloudTasksClient.create()) {
      String commaSepList = String.join(IDS_STRING_SPLIT, idsAsString);
      Task.Builder taskBuilder =
          Task.newBuilder()
              .setAppEngineHttpRequest(
                  AppEngineHttpRequest.newBuilder()
                      .setRelativeUri(taskUri)
                      .setBody(ByteString.copyFrom(commaSepList.getBytes()))
                      .setHttpMethod(HttpMethod.POST)
                      .build());

      Task task = client.createTask(queuePath, taskBuilder.build());

    } catch (IOException ex) {
      log.severe(
          String.format(
              "Error while creating task to push to queue for IDS %s and path %s",
              idsAsString, taskUri));
    }
  }

  /**
   * This endpoint will be called by the task in queue. The task will contain user ID whose
   * information needs to be send to RdrExportService
   *
   * @param request: Type: ByteString will contain comma separated User ids
   * @return
   */
  @Override
  public ResponseEntity<Void> exportResearcherData(Object request) {
    if (request == null || ((ByteString) request).isEmpty()) {
      log.severe(" call to export Researcher Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList =
        Arrays.asList(((ByteString) request).toStringUtf8().split(IDS_STRING_SPLIT)).stream()
            .map(strUserId -> Long.parseLong(strUserId))
            .collect(Collectors.toList());
    rdrExportService.exportUsers(requestUserIdList);

    return ResponseEntity.noContent().build();
  }

  /**
   * Send all the IDS passed in request body to RDRService
   *
   * @param researchIds: Type: ByteString will contain comma separated Workspace ids
   * @return
   */
  @Override
  public ResponseEntity<Void> exportWorkspaceData(Object researchIds) {
    if (researchIds == null || ((ByteString) researchIds).isEmpty()) {
      log.severe(" call to export Workspace Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList =
        Arrays.asList(((ByteString) researchIds).toStringUtf8().split(IDS_STRING_SPLIT)).stream()
            .map(strUserId -> Long.parseLong(strUserId))
            .collect(Collectors.toList());
    rdrExportService.exportWorkspaces(requestUserIdList);

    return ResponseEntity.noContent().build();
  }
}
