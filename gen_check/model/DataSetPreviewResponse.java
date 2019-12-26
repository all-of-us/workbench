package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.Domain;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetPreviewResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class DataSetPreviewResponse   {
  @JsonProperty("domain")
  private Domain domain = null;

  @JsonProperty("values")
  private List<DataSetPreviewValueList> values = null;

  public DataSetPreviewResponse domain(Domain domain) {
    this.domain = domain;
    return this;
  }

   /**
   * Get domain
   * @return domain
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Domain getDomain() {
    return domain;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public DataSetPreviewResponse values(List<DataSetPreviewValueList> values) {
    this.values = values;
    return this;
  }

  public DataSetPreviewResponse addValuesItem(DataSetPreviewValueList valuesItem) {
    if (this.values == null) {
      this.values = new ArrayList<DataSetPreviewValueList>();
    }
    this.values.add(valuesItem);
    return this;
  }

   /**
   * Get values
   * @return values
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<DataSetPreviewValueList> getValues() {
    return values;
  }

  public void setValues(List<DataSetPreviewValueList> values) {
    this.values = values;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetPreviewResponse dataSetPreviewResponse = (DataSetPreviewResponse) o;
    return Objects.equals(this.domain, dataSetPreviewResponse.domain) &&
        Objects.equals(this.values, dataSetPreviewResponse.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, values);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetPreviewResponse {\n");
    
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
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

