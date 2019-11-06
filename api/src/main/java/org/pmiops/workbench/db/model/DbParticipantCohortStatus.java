package org.pmiops.workbench.db.model;

import java.sql.Date;
import java.util.Objects;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.pmiops.workbench.model.CohortStatus;

@Entity
@Table(name = "participant_cohort_status")
public class DbParticipantCohortStatus {

  // Important: Keep fields in sync with ParticipantCohortStatusDao.ALL_COLUMNS_EXCEPT_REVIEW_ID.
  private DbParticipantCohortStatusKey participantKey;
  private Short status;
  private Long genderConceptId;
  private String gender;
  private Date birthDate;
  private Long raceConceptId;
  private String race;
  private Long ethnicityConceptId;
  private String ethnicity;
  private boolean deceased;

  @EmbeddedId
  @AttributeOverrides({
    @AttributeOverride(name = "cohortReviewId", column = @Column(name = "cohort_review_id")),
    @AttributeOverride(name = "participantId", column = @Column(name = "participant_id"))
  })
  public DbParticipantCohortStatusKey getParticipantKey() {
    return participantKey;
  }

  public void setParticipantKey(DbParticipantCohortStatusKey participantKey) {
    this.participantKey = participantKey;
  }

  public DbParticipantCohortStatus participantKey(DbParticipantCohortStatusKey participantKey) {
    this.participantKey = participantKey;
    return this;
  }

  @Column(name = "status")
  public Short getStatus() {
    return status;
  }

  public void setStatus(Short status) {
    this.status = status;
  }

  public DbParticipantCohortStatus status(Short status) {
    this.status = status;
    return this;
  }

  @Transient
  public CohortStatus getStatusEnum() {
    return DbStorageEnums.cohortStatusFromStorage(getStatus());
  }

  public void setStatusEnum(CohortStatus status) {
    setStatus(DbStorageEnums.cohortStatusToStorage(status));
  }

  public DbParticipantCohortStatus statusEnum(CohortStatus status) {
    return this.status(DbStorageEnums.cohortStatusToStorage(status));
  }

  @Column(name = "gender_concept_id")
  public Long getGenderConceptId() {
    return genderConceptId;
  }

  public void setGenderConceptId(Long genderConceptId) {
    this.genderConceptId = genderConceptId;
  }

  public DbParticipantCohortStatus genderConceptId(Long genderConceptId) {
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

  public DbParticipantCohortStatus gender(String gender) {
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

  public DbParticipantCohortStatus birthDate(Date birthDate) {
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

  public DbParticipantCohortStatus raceConceptId(Long raceConceptId) {
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

  public DbParticipantCohortStatus race(String race) {
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

  public DbParticipantCohortStatus ethnicityConceptId(Long ethnicityConceptId) {
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

  public DbParticipantCohortStatus ethnicity(String ethnicity) {
    this.ethnicity = ethnicity;
    return this;
  }

  @Column(name = "deceased")
  public boolean getDeceased() {
    return deceased;
  }

  public void setDeceased(boolean deceased) {
    this.deceased = deceased;
  }

  public DbParticipantCohortStatus deceased(boolean deceased) {
    this.deceased = deceased;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbParticipantCohortStatus that = (DbParticipantCohortStatus) o;
    return status == that.status
        && Objects.equals(genderConceptId, that.genderConceptId)
        && Objects.equals(gender, that.gender)
        && Objects.equals(birthDate, that.birthDate)
        && Objects.equals(raceConceptId, that.raceConceptId)
        && Objects.equals(race, that.race)
        && Objects.equals(ethnicityConceptId, that.ethnicityConceptId)
        && Objects.equals(ethnicity, that.ethnicity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        status,
        genderConceptId,
        gender,
        birthDate,
        raceConceptId,
        race,
        ethnicityConceptId,
        ethnicity);
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
