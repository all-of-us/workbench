package org.pmiops.workbench.api;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Payload.StringPayload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableMap;
import java.sql.Struct;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.StatusResponse;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;

  @Autowired
  StatusController(
      FireCloudService fireCloudService, LeonardoNotebooksClient leonardoNotebooksClient) {
    this.fireCloudService = fireCloudService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    testStackDriverLogging();
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonardoNotebooksClient.getNotebooksStatus());
    return ResponseEntity.ok(statusResponse);
  }

  private void testStackDriverLogging() {
    Logging logging = LoggingOptions.getDefaultInstance().getService();

    // The name of the log to write to
    String logName = "action-audit-test";

    // The data to write to the log
    String text = "Hello, world!";

    LogEntry stringEntry = LogEntry.newBuilder(StringPayload.of(text))
        .setSeverity(Severity.INFO)
        .setLogName(logName)
        .setResource(MonitoredResource.newBuilder("global").build())
        .build();

    // Writes the log entry asynchronously
    logging.write(Collections.singleton(stringEntry));

    Map<String, ?> data = ImmutableMap.of(
        "name", "Bond, James Bond",
        "occupation", "007",
        "shaken", true,
        "stirred", false,
        "numFilms", 25);

    LogEntry jsonEntry = LogEntry.newBuilder(JsonPayload.of(data))
        .setSeverity(Severity.INFO)
        .setLogName(logName)
        .setResource(MonitoredResource.newBuilder("global").build())
        .build();
    logging.write(Collections.singleton(jsonEntry));

    System.out.printf("Logged: %s%n", text);
  }
}
