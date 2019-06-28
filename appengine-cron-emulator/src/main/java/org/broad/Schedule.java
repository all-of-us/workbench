package org.broad;

import java.util.concurrent.TimeUnit;

class Schedule {

  public TimeUnit timeUnit;
  public int period;

  public Schedule(TimeUnit timeUnit, int period) {
    this.timeUnit = timeUnit;
    this.period = period;
  }
}
