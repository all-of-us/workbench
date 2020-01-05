package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Vocabulary
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class Vocabulary   {
  @JsonProperty("type")
  private String type = null;

  @JsonProperty("domain")
  private String domain = null;

  @JsonProperty("vocabulary")
  private String vocabulary = null;

  public Vocabulary type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Source or Standard
   * @return type
  **/
  @ApiModelProperty(required = true, value = "Source or Standard")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Vocabulary domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * OMOP domain
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "OMOP domain")
  @NotNull


  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Vocabulary vocabulary(String vocabulary) {
    this.vocabulary = vocabulary;
    return this;
  }

   /**
   * Vocabulary
   * @return vocabulary
  **/
  @ApiModelProperty(required = true, value = "Vocabulary")
  @NotNull


  public String getVocabulary() {
    return vocabulary;
  }

  public void setVocabulary(String vocabulary) {
    this.vocabulary = vocabulary;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Vocabulary vocabulary = (Vocabulary) o;
    return Objects.equals(this.type, vocabulary.type) &&
        Objects.equals(this.domain, vocabulary.domain) &&
        Objects.equals(this.vocabulary, vocabulary.vocabulary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, domain, vocabulary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Vocabulary {\n");
    
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    vocabulary: ").append(toIndentedString(vocabulary)).append("\n");
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

