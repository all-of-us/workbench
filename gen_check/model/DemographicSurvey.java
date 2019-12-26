package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.Gender;
import org.pmiops.workbench.model.Race;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DemographicSurvey
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class DemographicSurvey   {
  @JsonProperty("race")
  private List<Race> race = null;

  @JsonProperty("ethnicity")
  private Ethnicity ethnicity = null;

  @JsonProperty("gender")
  private List<Gender> gender = null;

  @JsonProperty("yearOfBirth")
  private BigDecimal yearOfBirth = null;

  @JsonProperty("education")
  private Education education = null;

  @JsonProperty("disability")
  private Boolean disability = false;

  public DemographicSurvey race(List<Race> race) {
    this.race = race;
    return this;
  }

  public DemographicSurvey addRaceItem(Race raceItem) {
    if (this.race == null) {
      this.race = new ArrayList<Race>();
    }
    this.race.add(raceItem);
    return this;
  }

   /**
   * Get race
   * @return race
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<Race> getRace() {
    return race;
  }

  public void setRace(List<Race> race) {
    this.race = race;
  }

  public DemographicSurvey ethnicity(Ethnicity ethnicity) {
    this.ethnicity = ethnicity;
    return this;
  }

   /**
   * Get ethnicity
   * @return ethnicity
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Ethnicity getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(Ethnicity ethnicity) {
    this.ethnicity = ethnicity;
  }

  public DemographicSurvey gender(List<Gender> gender) {
    this.gender = gender;
    return this;
  }

  public DemographicSurvey addGenderItem(Gender genderItem) {
    if (this.gender == null) {
      this.gender = new ArrayList<Gender>();
    }
    this.gender.add(genderItem);
    return this;
  }

   /**
   * Get gender
   * @return gender
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<Gender> getGender() {
    return gender;
  }

  public void setGender(List<Gender> gender) {
    this.gender = gender;
  }

  public DemographicSurvey yearOfBirth(BigDecimal yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
    return this;
  }

   /**
   * Get yearOfBirth
   * @return yearOfBirth
  **/
  @ApiModelProperty(value = "")

  @Valid

  public BigDecimal getYearOfBirth() {
    return yearOfBirth;
  }

  public void setYearOfBirth(BigDecimal yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
  }

  public DemographicSurvey education(Education education) {
    this.education = education;
    return this;
  }

   /**
   * Get education
   * @return education
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Education getEducation() {
    return education;
  }

  public void setEducation(Education education) {
    this.education = education;
  }

  public DemographicSurvey disability(Boolean disability) {
    this.disability = disability;
    return this;
  }

   /**
   * Get disability
   * @return disability
  **/
  @ApiModelProperty(value = "")


  public Boolean getDisability() {
    return disability;
  }

  public void setDisability(Boolean disability) {
    this.disability = disability;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DemographicSurvey demographicSurvey = (DemographicSurvey) o;
    return Objects.equals(this.race, demographicSurvey.race) &&
        Objects.equals(this.ethnicity, demographicSurvey.ethnicity) &&
        Objects.equals(this.gender, demographicSurvey.gender) &&
        Objects.equals(this.yearOfBirth, demographicSurvey.yearOfBirth) &&
        Objects.equals(this.education, demographicSurvey.education) &&
        Objects.equals(this.disability, demographicSurvey.disability);
  }

  @Override
  public int hashCode() {
    return Objects.hash(race, ethnicity, gender, yearOfBirth, education, disability);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DemographicSurvey {\n");
    
    sb.append("    race: ").append(toIndentedString(race)).append("\n");
    sb.append("    ethnicity: ").append(toIndentedString(ethnicity)).append("\n");
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    yearOfBirth: ").append(toIndentedString(yearOfBirth)).append("\n");
    sb.append("    education: ").append(toIndentedString(education)).append("\n");
    sb.append("    disability: ").append(toIndentedString(disability)).append("\n");
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

