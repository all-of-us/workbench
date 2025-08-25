package org.pmiops.workbench.absorb;

import jakarta.annotation.Nullable;
import java.time.Instant;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Enrollment {
  public final String courseId;
  @Nullable public final Instant completionTime;
  @Nullable public final Integer score;

  public Enrollment(String courseId, @Nullable Instant dateCompleted, @Nullable Integer score) {
    this.courseId = courseId;
    this.completionTime = dateCompleted;
    this.score = score;
  }

  public String toString() {
    return new ToStringBuilder(this)
        .append("courseId", courseId)
        .append("completionTime", completionTime)
        .append("score", score)
        .toString();
  }
}
