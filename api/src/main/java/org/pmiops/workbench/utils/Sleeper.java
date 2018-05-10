package org.pmiops.workbench.utils;

import org.springframework.stereotype.Service;

/**
 * Used for sleeping. Can be mocked out to avoid sleeps in tests.
 */
@Service
public class Sleeper {

  public void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
