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
import org.pmiops.workbench.rdr.RDRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/*
This offline process is responsible to daily sync up with RDR by sending all the created/modified
User and workspace information. Apart from logging error, the lastExport time is mantained in the
Database
 */
@RestController
public class OfflineRDRExportController implements OfflineRDRExportApiDelegate {

  private static final Logger log = Logger.getLogger(OfflineRDRExportController.class.getName());
  private RDRService rdrService;
  @Autowired private Provider<WorkbenchConfig> workbenchConfigProvider;

  private final String locationId = "us-central1";
  private final String queueName = "rdrQueueTest";

  private final String basePath = "/v1";
  private final String syncResearcherPath = basePath + "/syncResearcherData";
  private final String syncWorkspacePath = basePath + "/syncWorkspaceData";

  private final String stringSplit = ", ";

  OfflineRDRExportController(RDRService rdrService, Provider<WorkbenchConfig> configProvider) {
    this.rdrService = rdrService;
    this.workbenchConfigProvider = configProvider;
  }

  /*
  Purpose of this method will be to create multiple task of 2 categories, user (researcher) and
  workspace. Each task will contain 10 entities.
   */
  @Override
  public ResponseEntity<Void> syncWithRDR() {
    // Its important to send all researcher information first to RDR before sending workspace since
    // workspace object will contain collaborator information (userId)
    if (workbenchConfigProvider.get().featureFlags.enableRDRExport) {
      try {
        List<Long> userIdList = rdrService.findAllUserIdsToExport();
        groupIdsAndPushTask(userIdList, syncResearcherPath);
      } catch (Exception ex) {
        log.severe("Error while exporting researcher data to RDR");
      }
      try {
        List<Long> workspaceIdList = rdrService.findAllWorkspacesIdsToExport();
        groupIdsAndPushTask(workspaceIdList, syncWorkspacePath);
      } catch (Exception ex) {
        log.severe("Error while exporting workspace data to RDR");
      }
    }
    return ResponseEntity.noContent().build();
  }

  // For the list of Ids passed crete group of 10 ids and create task and push them in queue
  private void groupIdsAndPushTask(List<Long> idList, String taskUri) {
    final AtomicInteger counter = new AtomicInteger();

    final Collection<List<Long>> idGroups =
        idList.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 10))
            .values();
    Iterator<List<Long>> idIterator = idGroups.iterator();
    String queuePath =
        QueueName.of(workbenchConfigProvider.get().rdrServer.queueName, locationId, queueName)
            .toString();
    while (idIterator.hasNext()) {
      createAndPushTask(idIterator.next(), queuePath, taskUri);
    }
  }

  private void createAndPushTask(List<Long> ids, String queuePath, String taskUri) {
    List<String> idAsString = ids.stream().map(id -> id.toString()).collect(Collectors.toList());
    try (CloudTasksClient client = CloudTasksClient.create()) {
      String commaSepList = String.join(stringSplit, idAsString);
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
              idAsString, taskUri));
    }
  }

  /* This endpoint will be called by the task in queue. The task will contain user ID whose
  information
  needs to be send to RDRService
  */
  @Override
  public ResponseEntity<Void> syncResearcherData(Object request) {
    if (request == null || ((ByteString) request).isEmpty()) {
      log.severe(" call to sync Researcher Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList =
        Arrays.asList(((ByteString) request).toString().split(stringSplit)).stream()
            .map(strUserId -> Long.parseLong(strUserId))
            .collect(Collectors.toList());
    rdrService.sendUser(requestUserIdList);

    return ResponseEntity.noContent().build();
  }

  /*
  Send all the IDS passed in request body to RDRService
   */
  @Override
  public ResponseEntity<Void> syncWorkspaceData(Object researchIds) {
    if (researchIds == null || ((ByteString) researchIds).isEmpty()) {
      log.severe(" call to sync Workspace Data had no Ids");
      return ResponseEntity.noContent().build();
    }
    List<Long> requestUserIdList =
        Arrays.asList(((ByteString) researchIds).toString().split(stringSplit)).stream()
            .map(strUserId -> Long.parseLong(strUserId))
            .collect(Collectors.toList());
    rdrService.sendWorkspace(requestUserIdList);

    return ResponseEntity.noContent().build();
  }
}
