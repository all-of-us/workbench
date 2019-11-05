package org.pmiops.workbench.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Embeddable
public class DbParticipantCohortStatusKey implements Serializable {

  @Column(name = "cohort_review_id")
  private long cohortReviewId;

  @Column(name = "participant_id")
  private long participantId;

  public DbParticipantCohortStatusKey() {}

  public DbParticipantCohortStatusKey(long cohortReviewId, long participantId) {
    this.cohortReviewId = cohortReviewId;
    this.participantId = participantId;
  }

  public long getCohortReviewId() {
    return cohortReviewId;
  }

  public void setCohortReviewId(long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
  }

  public DbParticipantCohortStatusKey cohortReviewId(long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
    return this;
  }

  public long getParticipantId() {
    return participantId;
  }

  public void setParticipantId(long participantId) {
    this.participantId = participantId;
  }

  public DbParticipantCohortStatusKey participantId(long participantId) {
    this.participantId = participantId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbParticipantCohortStatusKey that = (DbParticipantCohortStatusKey) o;
    return cohortReviewId == that.cohortReviewId && participantId == that.participantId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortReviewId, participantId);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("cohortReviewId", cohortReviewId)
        .append("participantId", participantId)
        .toString();
  }
}
