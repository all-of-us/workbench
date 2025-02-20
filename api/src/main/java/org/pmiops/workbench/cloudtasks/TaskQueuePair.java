package org.pmiops.workbench.cloudtasks;

public record TaskQueuePair(String queueName, String endpoint) {
  private static final String BASE_PATH = "/v1/cloudTask";

  String fullPath() {
    return String.format("%s/%s", BASE_PATH, endpoint);
  }
}
