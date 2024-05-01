package org.pmiops.workbench.absorb;

import java.time.Instant;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Enrollment {
  public final String courseId;
  @Nullable public final Instant completionTime;

  public Enrollment(String courseId, @Nullable Instant dateCompleted) {
    this.courseId = courseId;
    this.completionTime = dateCompleted;
  }

  public String toString() {
    return new ToStringBuilder(this)
        .append("courseId", courseId)
        .append("completionTime", completionTime)
        .toString();
  }
}
