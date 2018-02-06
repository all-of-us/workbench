package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.CohortStatus;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "participant_cohort_status")
public class ParticipantCohortStatus {

    // Important: Keep fields in sync with ParticipantCohortStatusDao.ALL_COLUMNS_EXCEPT_REVIEW_ID.
    private ParticipantCohortStatusKey participantKey;
    private CohortStatus status;
    private Long genderConceptId;
    private String gender;
    private Date birthDate;
    private Long raceConceptId;
    private String race;
    private Long ethnicityConceptId;
    private String ethnicity;

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name="cohortReviewId",
                    column=@Column(name="cohort_review_id")),
            @AttributeOverride(name="participantId",
                    column=@Column(name="participant_id"))
    })
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

    @Column(name = "gender_concept_id")
    public Long getGenderConceptId() {
        return genderConceptId;
    }

    public void setGenderConceptId(Long genderConceptId) {
        this.genderConceptId = genderConceptId;
    }

    public ParticipantCohortStatus genderConceptId(Long genderConceptId) {
        this.genderConceptId = genderConceptId;
        return this;
    }

    @Transient
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

    @Column(name = "race_concept_id")
    public Long getRaceConceptId() {
        return raceConceptId;
    }

    public void setRaceConceptId(Long raceConceptId) {
        this.raceConceptId = raceConceptId;
    }

    public ParticipantCohortStatus raceConceptId(Long raceConceptId) {
        this.raceConceptId = raceConceptId;
        return this;
    }

    @Transient
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

    @Column(name = "ethnicity_concept_id")
    public Long getEthnicityConceptId() {
        return ethnicityConceptId;
    }

    public void setEthnicityConceptId(Long ethnicityConceptId) {
        this.ethnicityConceptId = ethnicityConceptId;
    }

    public ParticipantCohortStatus ethnicityConceptId(Long ethnicityConceptId) {
        this.ethnicityConceptId = ethnicityConceptId;
        return this;
    }

    @Transient
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
        return status == that.status &&
                Objects.equals(genderConceptId, that.genderConceptId) &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(birthDate, that.birthDate) &&
                Objects.equals(raceConceptId, that.raceConceptId) &&
                Objects.equals(race, that.race) &&
                Objects.equals(ethnicityConceptId, that.ethnicityConceptId) &&
                Objects.equals(ethnicity, that.ethnicity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, genderConceptId, gender, birthDate, raceConceptId, race, ethnicityConceptId, ethnicity);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("participantKey", participantKey)
                .append("status", status)
                .append("genderConceptId", genderConceptId)
                .append("gender", gender)
                .append("birthDate", birthDate)
                .append("raceConceptId", raceConceptId)
                .append("race", race)
                .append("ethnicityConceptId", ethnicityConceptId)
                .append("ethnicity", ethnicity)
                .toString();
    }
}
