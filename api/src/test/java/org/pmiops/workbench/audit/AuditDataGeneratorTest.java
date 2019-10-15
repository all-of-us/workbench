package org.pmiops.workbench.audit;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.audit.synthetic.AuditDataGenerator;

public class AuditDataGeneratorTest {

  @Test
  public void itGetsLongInRange() {
    final long low = 1000L;
    final long high = 2000L;
    final long random = AuditDataGenerator.randomLongInRange(low, high);
    assertThat(low <= random).isTrue();
    assertThat(random < high).isTrue();
  }
}
