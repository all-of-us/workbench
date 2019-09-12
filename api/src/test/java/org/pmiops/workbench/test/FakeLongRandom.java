package org.pmiops.workbench.test;

import java.util.Random;

/** Stubbed Random implementation for testing. */
public class FakeLongRandom extends Random {
  private final long value;

  public FakeLongRandom(long value) {
    this.value = value;
  }

  @Override
  public long nextLong() {
    return value;
  }
}
