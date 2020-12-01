package org.pmiops.workbench.utils;

import org.springframework.http.ResponseEntity;

public class ResponseEntities {

  /**
   * Simple wrapper for methods that return 204/No Content.
   *
   * @param runnable - action to perform
   * @return 204 ResponseEntity
   */
  public static ResponseEntity<Void> noContentRun(Runnable runnable) {
    runnable.run();
    return ResponseEntity.noContent().build();
  }
}
