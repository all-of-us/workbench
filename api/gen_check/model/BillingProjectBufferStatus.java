package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * BillingProjectBufferStatus
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class BillingProjectBufferStatus   {
  @JsonProperty("bufferSize")
  private Long bufferSize = null;

  public BillingProjectBufferStatus bufferSize(Long bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

   /**
   * Current size of the billing buffer, i.e. the number of projects ready to be claimed. 
   * @return bufferSize
  **/
  @ApiModelProperty(value = "Current size of the billing buffer, i.e. the number of projects ready to be claimed. ")


  public Long getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(Long bufferSize) {
    this.bufferSize = bufferSize;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BillingProjectBufferStatus billingProjectBufferStatus = (BillingProjectBufferStatus) o;
    return Objects.equals(this.bufferSize, billingProjectBufferStatus.bufferSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bufferSize);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BillingProjectBufferStatus {\n");
    
    sb.append("    bufferSize: ").append(toIndentedString(bufferSize)).append("\n");
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

