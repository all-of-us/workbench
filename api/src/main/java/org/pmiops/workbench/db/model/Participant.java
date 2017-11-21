package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.CohortStatus;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "participant_cohort_status")
public class Participant {

    private ParticipantKey participantKey;
    private CohortStatus status;

    @EmbeddedId
    public ParticipantKey getParticipantKey() {
        return participantKey;
    }

    public void setParticipantKey(ParticipantKey participantKey) {
        this.participantKey = participantKey;
    }

    public Participant participantKey(ParticipantKey participantKey) {
        this.participantKey = participantKey;
        return this;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public CohortStatus getStatus() {
        return status;
    }

    public void setStatus(CohortStatus status) {
        this.status = status;
    }

    public Participant status(CohortStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(participantKey.getParticipantId(), that.participantKey.getParticipantId()) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(participantKey.getParticipantId(), status);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("participantKey", participantKey)
                .append("status", status)
                .toString();
    }
}
