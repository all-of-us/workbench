package org.pmiops.workbench.testconfig;

import com.google.common.base.Ticker;
import java.util.concurrent.TimeUnit;

public class FakeTicker extends Ticker {
  private long currentNanos;

  protected FakeTicker(long tickNanos) {
    super();
    this.currentNanos = tickNanos;
  }

  @Override
  public long read() {
    currentNanos += TimeUnit.MILLISECONDS.toNanos(200);
    return currentNanos;
  }
}
