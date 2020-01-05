package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Modifier
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class Modifier   {
  @JsonProperty("name")
  private ModifierType name = null;

  @JsonProperty("operator")
  private Operator operator = null;

  @JsonProperty("operands")
  private List<String> operands = new ArrayList<String>();

  public Modifier name(ModifierType name) {
    this.name = name;
    return this;
  }

   /**
   * name/type of modifier
   * @return name
  **/
  @ApiModelProperty(required = true, value = "name/type of modifier")
  @NotNull

  @Valid

  public ModifierType getName() {
    return name;
  }

  public void setName(ModifierType name) {
    this.name = name;
  }

  public Modifier operator(Operator operator) {
    this.operator = operator;
    return this;
  }

   /**
   * Machine name of the operator
   * @return operator
  **/
  @ApiModelProperty(required = true, value = "Machine name of the operator")
  @NotNull

  @Valid

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public Modifier operands(List<String> operands) {
    this.operands = operands;
    return this;
  }

  public Modifier addOperandsItem(String operandsItem) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Modifier modifier = (Modifier) o;
    return Objects.equals(this.name, modifier.name) &&
        Objects.equals(this.operator, modifier.operator) &&
        Objects.equals(this.operands, modifier.operands);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, operator, operands);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Modifier {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    operands: ").append(toIndentedString(operands)).append("\n");
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

