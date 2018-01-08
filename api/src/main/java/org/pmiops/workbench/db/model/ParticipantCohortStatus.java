package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.CohortStatus;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "participant_cohort_status")
public class ParticipantCohortStatus {

    private ParticipantCohortStatusKey participantKey;
    private CohortStatus status;
    private String gender;
    private Date birthDate;
    private String race;
    private String ethnicity;

    @EmbeddedId
    public ParticipantCohortStatusKey getParticipantKey() {
        return participantKey;
    }

    public void setParticipantKey(ParticipantCohortStatusKey participantKey) {
        this.participantKey = participantKey;
    }

    public ParticipantCohortStatus participantKey(ParticipantCohortStatusKey participantKey) {
        this.participantKey = participantKey;
        return this;
    }

    @Column(name = "status")
    public CohortStatus getStatus() {
        return status;
    }

    public void setStatus(CohortStatus status) {
        this.status = status;
    }

    public ParticipantCohortStatus status(CohortStatus status) {
        this.status = status;
        return this;
    }

    @Column(name = "gender")
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public ParticipantCohortStatus gender(String gender) {
        this.gender = gender;
        return this;
    }

    @Column(name = "birth_date")
    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public ParticipantCohortStatus birthDate(Date birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    @Column(name = "race")
    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public ParticipantCohortStatus race(String race) {
        this.race = race;
        return this;
    }

    @Column(name = "ethnicity")
    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public ParticipantCohortStatus ethnicity(String ethnicity) {
        this.ethnicity = ethnicity;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantCohortStatus that = (ParticipantCohortStatus) o;
        return Objects.equals(participantKey, that.participantKey) &&
                status == that.status &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(birthDate, that.birthDate) &&
                Objects.equals(race, that.race) &&
                Objects.equals(ethnicity, that.ethnicity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participantKey, status, gender, birthDate, race, ethnicity);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("participantKey", participantKey)
                .append("status", status)
                .append("gender", gender)
                .append("birthDate", birthDate)
                .append("race", race)
                .append("ethnicity", ethnicity)
                .toString();
    }
}
