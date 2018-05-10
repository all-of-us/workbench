package org.pmiops.workbench.test;

public class FakeSleeper extends Sleeper {

  @Override
  public void sleep(long millis) throws InterruptedException {
    // Do nothing.
  }
}
