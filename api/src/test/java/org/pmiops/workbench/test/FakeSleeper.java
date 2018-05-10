package org.pmiops.workbench.test;

import org.pmiops.workbench.utils.Sleeper;

public class FakeSleeper extends Sleeper {

  @Override
  public void sleep(long millis) throws InterruptedException {
    // Do nothing.
  }
}
