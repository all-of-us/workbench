package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParticipantCohortStatusKey implements Serializable {

    @Column(name = "cohort_id")
    private long cohortId;

    @Column(name = "cdr_version_id")
    private long cdrVersionId;

    @Column(name = "participant_id")
    private long participantId;

    public ParticipantCohortStatusKey() {
    }

    public ParticipantCohortStatusKey(long cohortId, long cdrVersionId, long participantId) {
        this.cohortId = cohortId;
        this.cdrVersionId = cdrVersionId;
        this.participantId = participantId;
    }

    public long getCohortId() {
        return cohortId;
    }

    public void setCohortId(long cohortId) {
        this.cohortId = cohortId;
    }

    public ParticipantCohortStatusKey cohortId(long cohortId) {
        this.cohortId = cohortId;
        return this;
    }

    public long getCdrVersionId() {
        return cdrVersionId;
    }

    public void setCdrVersionId(long cdrVersionId) {
        this.cdrVersionId = cdrVersionId;
    }

    public ParticipantCohortStatusKey cdrVersionId(long cdrVersionId) {
        this.cdrVersionId = cdrVersionId;
        return this;
    }

    public long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(long participantId) {
        this.participantId = participantId;
    }

    public ParticipantCohortStatusKey participantId(long participantId) {
        this.participantId = participantId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantCohortStatusKey that = (ParticipantCohortStatusKey) o;
        return cohortId == that.cohortId &&
                cdrVersionId == that.cdrVersionId &&
                participantId == that.participantId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cohortId, cdrVersionId, participantId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cohortId", cohortId)
                .append("cdrVersionId", cdrVersionId)
                .append("participantId", participantId)
                .toString();
    }
}
