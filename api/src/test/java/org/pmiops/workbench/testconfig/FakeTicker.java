package org.pmiops.workbench.testconfig;

import com.google.common.base.Ticker;

public class FakeTicker extends Ticker {
  private final long elapsedMillis;

  protected FakeTicker(long elapsedMillis) {
    super();
    this.elapsedMillis = elapsedMillis;
  }

  @Override
  public long read() {
    return elapsedMillis;
  }
}
