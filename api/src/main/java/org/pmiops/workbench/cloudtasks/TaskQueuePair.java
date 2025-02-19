package org.pmiops.workbench.cloudtasks;

public record TaskQueuePair(String queueName, String endpoint) {
  private static final String BASE_PATH = "/v1/cloudTask";

  static String fullPath(String path) {
    return String.format("%s/%s", BASE_PATH, path);
  }

  String fullPath() {
    return fullPath(endpoint);
  }
}
