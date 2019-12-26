package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.CohortStatus;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantCohortStatus
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class ParticipantCohortStatus   {
  @JsonProperty("participantId")
  private Long participantId = null;

  @JsonProperty("status")
  private CohortStatus status = null;

  @JsonProperty("genderConceptId")
  private Long genderConceptId = null;

  @JsonProperty("gender")
  private String gender = null;

  @JsonProperty("birthDate")
  private String birthDate = null;

  @JsonProperty("raceConceptId")
  private Long raceConceptId = null;

  @JsonProperty("race")
  private String race = null;

  @JsonProperty("ethnicityConceptId")
  private Long ethnicityConceptId = null;

  @JsonProperty("ethnicity")
  private String ethnicity = null;

  @JsonProperty("deceased")
  private Boolean deceased = false;

  public ParticipantCohortStatus participantId(Long participantId) {
    this.participantId = participantId;
    return this;
  }

   /**
   * Get participantId
   * @return participantId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getParticipantId() {
    return participantId;
  }

  public void setParticipantId(Long participantId) {
    this.participantId = participantId;
  }

  public ParticipantCohortStatus status(CohortStatus status) {
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public CohortStatus getStatus() {
    return status;
  }

  public void setStatus(CohortStatus status) {
    this.status = status;
  }

  public ParticipantCohortStatus genderConceptId(Long genderConceptId) {
    this.genderConceptId = genderConceptId;
    return this;
  }

   /**
   * Get genderConceptId
   * @return genderConceptId
  **/
  @ApiModelProperty(value = "")


  public Long getGenderConceptId() {
    return genderConceptId;
  }

  public void setGenderConceptId(Long genderConceptId) {
    this.genderConceptId = genderConceptId;
  }

  public ParticipantCohortStatus gender(String gender) {
    this.gender = gender;
    return this;
  }

   /**
   * Get gender
   * @return gender
  **/
  @ApiModelProperty(value = "")


  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public ParticipantCohortStatus birthDate(String birthDate) {
    this.birthDate = birthDate;
    return this;
  }

   /**
   * Get birthDate
   * @return birthDate
  **/
  @ApiModelProperty(value = "")


  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public ParticipantCohortStatus raceConceptId(Long raceConceptId) {
    this.raceConceptId = raceConceptId;
    return this;
  }

   /**
   * Get raceConceptId
   * @return raceConceptId
  **/
  @ApiModelProperty(value = "")


  public Long getRaceConceptId() {
    return raceConceptId;
  }

  public void setRaceConceptId(Long raceConceptId) {
    this.raceConceptId = raceConceptId;
  }

  public ParticipantCohortStatus race(String race) {
    this.race = race;
    return this;
  }

   /**
   * Get race
   * @return race
  **/
  @ApiModelProperty(value = "")


  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  public ParticipantCohortStatus ethnicityConceptId(Long ethnicityConceptId) {
    this.ethnicityConceptId = ethnicityConceptId;
    return this;
  }

   /**
   * Get ethnicityConceptId
   * @return ethnicityConceptId
  **/
  @ApiModelProperty(value = "")


  public Long getEthnicityConceptId() {
    return ethnicityConceptId;
  }

  public void setEthnicityConceptId(Long ethnicityConceptId) {
    this.ethnicityConceptId = ethnicityConceptId;
  }

  public ParticipantCohortStatus ethnicity(String ethnicity) {
    this.ethnicity = ethnicity;
    return this;
  }

   /**
   * Get ethnicity
   * @return ethnicity
  **/
  @ApiModelProperty(value = "")


  public String getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(String ethnicity) {
    this.ethnicity = ethnicity;
  }

  public ParticipantCohortStatus deceased(Boolean deceased) {
    this.deceased = deceased;
    return this;
  }

   /**
   * Get deceased
   * @return deceased
  **/
  @ApiModelProperty(value = "")


  public Boolean getDeceased() {
    return deceased;
  }

  public void setDeceased(Boolean deceased) {
    this.deceased = deceased;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantCohortStatus participantCohortStatus = (ParticipantCohortStatus) o;
    return Objects.equals(this.participantId, participantCohortStatus.participantId) &&
        Objects.equals(this.status, participantCohortStatus.status) &&
        Objects.equals(this.genderConceptId, participantCohortStatus.genderConceptId) &&
        Objects.equals(this.gender, participantCohortStatus.gender) &&
        Objects.equals(this.birthDate, participantCohortStatus.birthDate) &&
        Objects.equals(this.raceConceptId, participantCohortStatus.raceConceptId) &&
        Objects.equals(this.race, participantCohortStatus.race) &&
        Objects.equals(this.ethnicityConceptId, participantCohortStatus.ethnicityConceptId) &&
        Objects.equals(this.ethnicity, participantCohortStatus.ethnicity) &&
        Objects.equals(this.deceased, participantCohortStatus.deceased);
  }

  @Override
  public int hashCode() {
    return Objects.hash(participantId, status, genderConceptId, gender, birthDate, raceConceptId, race, ethnicityConceptId, ethnicity, deceased);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantCohortStatus {\n");
    
    sb.append("    participantId: ").append(toIndentedString(participantId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    genderConceptId: ").append(toIndentedString(genderConceptId)).append("\n");
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    birthDate: ").append(toIndentedString(birthDate)).append("\n");
    sb.append("    raceConceptId: ").append(toIndentedString(raceConceptId)).append("\n");
    sb.append("    race: ").append(toIndentedString(race)).append("\n");
    sb.append("    ethnicityConceptId: ").append(toIndentedString(ethnicityConceptId)).append("\n");
    sb.append("    ethnicity: ").append(toIndentedString(ethnicity)).append("\n");
    sb.append("    deceased: ").append(toIndentedString(deceased)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

