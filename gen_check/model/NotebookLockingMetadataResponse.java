package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * NotebookLockingMetadataResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class NotebookLockingMetadataResponse   {
  @JsonProperty("lastLockedBy")
  private String lastLockedBy = null;

  @JsonProperty("lockExpirationTime")
  private Long lockExpirationTime = null;

  public NotebookLockingMetadataResponse lastLockedBy(String lastLockedBy) {
    this.lastLockedBy = lastLockedBy;
    return this;
  }

   /**
   * Get lastLockedBy
   * @return lastLockedBy
  **/
  @ApiModelProperty(value = "")


  public String getLastLockedBy() {
    return lastLockedBy;
  }

  public void setLastLockedBy(String lastLockedBy) {
    this.lastLockedBy = lastLockedBy;
  }

  public NotebookLockingMetadataResponse lockExpirationTime(Long lockExpirationTime) {
    this.lockExpirationTime = lockExpirationTime;
    return this;
  }

   /**
   * The time when the lock will expire, in ms from the Unix epoch
   * @return lockExpirationTime
  **/
  @ApiModelProperty(value = "The time when the lock will expire, in ms from the Unix epoch")


  public Long getLockExpirationTime() {
    return lockExpirationTime;
  }

  public void setLockExpirationTime(Long lockExpirationTime) {
    this.lockExpirationTime = lockExpirationTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotebookLockingMetadataResponse notebookLockingMetadataResponse = (NotebookLockingMetadataResponse) o;
    return Objects.equals(this.lastLockedBy, notebookLockingMetadataResponse.lastLockedBy) &&
        Objects.equals(this.lockExpirationTime, notebookLockingMetadataResponse.lockExpirationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastLockedBy, lockExpirationTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotebookLockingMetadataResponse {\n");
    
    sb.append("    lastLockedBy: ").append(toIndentedString(lastLockedBy)).append("\n");
    sb.append("    lockExpirationTime: ").append(toIndentedString(lockExpirationTime)).append("\n");
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

