package org.pmiops.workbench.absorb;

import jakarta.annotation.Nullable;
import java.time.Instant;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Enrollment {
  public final String courseId;
  @Nullable public final Instant completionTime;
  @Nullable public final Integer score;
  @Nullable public final EnrollmentStatus status;

  public Enrollment(
      String courseId,
      @Nullable Instant dateCompleted,
      @Nullable Integer score,
      @Nullable EnrollmentStatus status) {
    this.courseId = courseId;
    this.completionTime = dateCompleted;
    this.score = score;
    this.status = status;
  }

  public String toString() {
    return new ToStringBuilder(this)
        .append("courseId", courseId)
        .append("completionTime", completionTime)
        .append("score", score)
        .append("status", status)
        .toString();
  }
}
