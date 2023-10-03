package org.pmiops.workbench.absorb;

import java.time.Instant;
import javax.annotation.Nullable;

public class Enrollment {
  public final String courseId;
  @Nullable public final Instant completionTime;

  public Enrollment(String courseId, @Nullable Instant dateCompleted) {
    this.courseId = courseId;
    this.completionTime = dateCompleted;
  }
}
