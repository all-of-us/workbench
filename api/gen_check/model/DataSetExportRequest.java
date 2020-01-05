package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.KernelTypeEnum;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataSetExportRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class DataSetExportRequest   {
  @JsonProperty("dataSetRequest")
  private DataSetRequest dataSetRequest = null;

  @JsonProperty("notebookName")
  private String notebookName = null;

  @JsonProperty("newNotebook")
  private Boolean newNotebook = false;

  @JsonProperty("kernelType")
  private KernelTypeEnum kernelType = null;

  public DataSetExportRequest dataSetRequest(DataSetRequest dataSetRequest) {
    this.dataSetRequest = dataSetRequest;
    return this;
  }

   /**
   * Get dataSetRequest
   * @return dataSetRequest
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public DataSetRequest getDataSetRequest() {
    return dataSetRequest;
  }

  public void setDataSetRequest(DataSetRequest dataSetRequest) {
    this.dataSetRequest = dataSetRequest;
  }

  public DataSetExportRequest notebookName(String notebookName) {
    this.notebookName = notebookName;
    return this;
  }

   /**
   * Get notebookName
   * @return notebookName
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getNotebookName() {
    return notebookName;
  }

  public void setNotebookName(String notebookName) {
    this.notebookName = notebookName;
  }

  public DataSetExportRequest newNotebook(Boolean newNotebook) {
    this.newNotebook = newNotebook;
    return this;
  }

   /**
   * Get newNotebook
   * @return newNotebook
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getNewNotebook() {
    return newNotebook;
  }

  public void setNewNotebook(Boolean newNotebook) {
    this.newNotebook = newNotebook;
  }

  public DataSetExportRequest kernelType(KernelTypeEnum kernelType) {
    this.kernelType = kernelType;
    return this;
  }

   /**
   * Get kernelType
   * @return kernelType
  **/
  @ApiModelProperty(value = "")

  @Valid

  public KernelTypeEnum getKernelType() {
    return kernelType;
  }

  public void setKernelType(KernelTypeEnum kernelType) {
    this.kernelType = kernelType;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataSetExportRequest dataSetExportRequest = (DataSetExportRequest) o;
    return Objects.equals(this.dataSetRequest, dataSetExportRequest.dataSetRequest) &&
        Objects.equals(this.notebookName, dataSetExportRequest.notebookName) &&
        Objects.equals(this.newNotebook, dataSetExportRequest.newNotebook) &&
        Objects.equals(this.kernelType, dataSetExportRequest.kernelType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataSetRequest, notebookName, newNotebook, kernelType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataSetExportRequest {\n");
    
    sb.append("    dataSetRequest: ").append(toIndentedString(dataSetRequest)).append("\n");
    sb.append("    notebookName: ").append(toIndentedString(notebookName)).append("\n");
    sb.append("    newNotebook: ").append(toIndentedString(newNotebook)).append("\n");
    sb.append("    kernelType: ").append(toIndentedString(kernelType)).append("\n");
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

