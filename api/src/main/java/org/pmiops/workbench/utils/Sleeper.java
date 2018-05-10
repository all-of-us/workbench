package org.pmiops.workbench.utils;

/**
 * Used for sleeping. Can be mocked out to avoid sleeps in tests.
 */
public class Sleeper {

  public void sleep(long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
