package org.pmiops.workbench.audit;

import org.junit.Assert;
import org.junit.Test;

public class AuditDataGeneratorTest {

  @Test
  public void itGetsLongInRange() {
    final long low = 1000L;
    final long high = 2000L;
    final long random = AuditDataGenerator.randomLongInRange(low, high);
    Assert.assertThat(low).isLess
  }
}
