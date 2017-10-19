package org.pmiops.workbench.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Mutable clock implementation for testing.
 */
public class FakeClock extends Clock {

  private Instant instant;
  private ZoneId zoneId;

  public FakeClock(Instant instant, ZoneId zoneId) {
    this.instant = instant;
    this.zoneId = zoneId;
  }

  public FakeClock(Instant instant) {
    this(instant, ZoneId.systemDefault());
  }

  public FakeClock() {
    this(Instant.now());
  }

  @Override
  public long millis() {
    return instant.toEpochMilli();
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Instant instant() {
    return instant;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new FakeClock(instant, zone);
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  public void increment(long millis) {
    setInstant(Instant.ofEpochMilli(this.instant.toEpochMilli() + millis));
  }
}
