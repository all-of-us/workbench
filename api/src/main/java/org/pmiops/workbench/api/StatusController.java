package org.pmiops.workbench.api;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.audit.synthetic.AuditDataGenerator;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.StatusResponse;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  public static final MonitoredResource LOGGING_RESOURCE =
      MonitoredResource.newBuilder("global").build();
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
    // testStackDriverLogging();
    populateAuditTestData();
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonardoNotebooksClient.getNotebooksStatus());
    return ResponseEntity.ok(statusResponse);
  }

  private void testStackDriverLogging() {
    Logging logging = LoggingOptions.getDefaultInstance().getService();

    // The name of the log to write to
    final String logName = "action-audit-test";

    final Map<String, ?> data =
        ImmutableMap.of(
            "name",
            "Bond, James Bond",
            "occupation",
            "007",
            "shaken",
            true,
            "stirred",
            false,
            "numFilms",
            25);

    LogEntry jsonEntry =
        LogEntry.newBuilder(JsonPayload.of(data))
            .setSeverity(Severity.INFO)
            .setLogName(logName)
            .setResource(MonitoredResource.newBuilder("global").build())
            .build();
    logging.write(Collections.singleton(jsonEntry));
  }

  private void populateAuditTestData() {
    Logging logging = LoggingOptions.getDefaultInstance().getService();
    List<LogEntry> entries = AuditDataGenerator.generateRandomLogEntries(1000);
    logging.write(entries);
  }
}
