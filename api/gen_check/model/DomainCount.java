package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Domain;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DomainCount
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class DomainCount   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("conceptCount")
  private Long conceptCount = null;

  public DomainCount domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * the domain ID
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "the domain ID")
  @NotNull

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DomainCount name(String name) {
    this.name = name;
    return this;
  }

   /**
   * display name of the domain
   * @return name
  **/
  @ApiModelProperty(required = true, value = "display name of the domain")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DomainCount conceptCount(Long conceptCount) {
    this.conceptCount = conceptCount;
    return this;
  }

   /**
   * number of concepts matching the search query in this domain
   * @return conceptCount
  **/
  @ApiModelProperty(required = true, value = "number of concepts matching the search query in this domain")
  @NotNull


  public Long getConceptCount() {
    return conceptCount;
  }

  public void setConceptCount(Long conceptCount) {
    this.conceptCount = conceptCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainCount domainCount = (DomainCount) o;
    return Objects.equals(this.domain, domainCount.domain) &&
        Objects.equals(this.name, domainCount.name) &&
        Objects.equals(this.conceptCount, domainCount.conceptCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, name, conceptCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DomainCount {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    conceptCount: ").append(toIndentedString(conceptCount)).append("\n");
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

