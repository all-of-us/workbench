package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.Operator;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Attribute
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class Attribute   {
  @JsonProperty("name")
  private AttrName name = null;

  @JsonProperty("operator")
  private Operator operator = null;

  @JsonProperty("operands")
  private List<String> operands = new ArrayList<String>();

  @JsonProperty("conceptId")
  private Long conceptId = null;

  public Attribute name(AttrName name) {
    this.name = name;
    return this;
  }

   /**
   * the name of the attribute
   * @return name
  **/
  @ApiModelProperty(required = true, value = "the name of the attribute")
  @NotNull

  @Valid

  public AttrName getName() {
    return name;
  }

  public void setName(AttrName name) {
    this.name = name;
  }

  public Attribute operator(Operator operator) {
    this.operator = operator;
    return this;
  }

   /**
   * Get operator
   * @return operator
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public Attribute operands(List<String> operands) {
    this.operands = operands;
    return this;
  }

  public Attribute addOperandsItem(String operandsItem) {
    this.operands.add(operandsItem);
    return this;
  }

   /**
   * Get operands
   * @return operands
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public List<String> getOperands() {
    return operands;
  }

  public void setOperands(List<String> operands) {
    this.operands = operands;
  }

  public Attribute conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * The concept id that maps to concept table.
   * @return conceptId
  **/
  @ApiModelProperty(value = "The concept id that maps to concept table.")


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Attribute attribute = (Attribute) o;
    return Objects.equals(this.name, attribute.name) &&
        Objects.equals(this.operator, attribute.operator) &&
        Objects.equals(this.operands, attribute.operands) &&
        Objects.equals(this.conceptId, attribute.conceptId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, operator, operands, conceptId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Attribute {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    operands: ").append(toIndentedString(operands)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
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

