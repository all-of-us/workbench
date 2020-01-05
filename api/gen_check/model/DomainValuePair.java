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
 * DomainValuePair
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class DomainValuePair   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("value")
  private String value = null;

  public DomainValuePair domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Domain corresponding to an OMOP table. 
   * @return domain
  **/
  @ApiModelProperty(value = "Domain corresponding to an OMOP table. ")

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DomainValuePair value(String value) {
    this.value = value;
    return this;
  }

   /**
   * Get value
   * @return value
  **/
  @ApiModelProperty(value = "")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainValuePair domainValuePair = (DomainValuePair) o;
    return Objects.equals(this.domain, domainValuePair.domain) &&
        Objects.equals(this.value, domainValuePair.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DomainValuePair {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

