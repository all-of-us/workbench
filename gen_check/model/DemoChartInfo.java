package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DemoChartInfo
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class DemoChartInfo   {
  @JsonProperty("gender")
  private String gender = null;

  @JsonProperty("race")
  private String race = null;

  @JsonProperty("ageRange")
  private String ageRange = null;

  @JsonProperty("count")
  private Long count = null;

  public DemoChartInfo gender(String gender) {
    this.gender = gender;
    return this;
  }

   /**
   * gender of subject
   * @return gender
  **/
  @ApiModelProperty(required = true, value = "gender of subject")
  @NotNull


  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public DemoChartInfo race(String race) {
    this.race = race;
    return this;
  }

   /**
   * race of subject
   * @return race
  **/
  @ApiModelProperty(required = true, value = "race of subject")
  @NotNull


  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  public DemoChartInfo ageRange(String ageRange) {
    this.ageRange = ageRange;
    return this;
  }

   /**
   * age range of subject
   * @return ageRange
  **/
  @ApiModelProperty(required = true, value = "age range of subject")
  @NotNull


  public String getAgeRange() {
    return ageRange;
  }

  public void setAgeRange(String ageRange) {
    this.ageRange = ageRange;
  }

  public DemoChartInfo count(Long count) {
    this.count = count;
    return this;
  }

   /**
   * number of subjects
   * @return count
  **/
  @ApiModelProperty(required = true, value = "number of subjects")
  @NotNull


  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DemoChartInfo demoChartInfo = (DemoChartInfo) o;
    return Objects.equals(this.gender, demoChartInfo.gender) &&
        Objects.equals(this.race, demoChartInfo.race) &&
        Objects.equals(this.ageRange, demoChartInfo.ageRange) &&
        Objects.equals(this.count, demoChartInfo.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gender, race, ageRange, count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DemoChartInfo {\n");
    
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    race: ").append(toIndentedString(race)).append("\n");
    sb.append("    ageRange: ").append(toIndentedString(ageRange)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

