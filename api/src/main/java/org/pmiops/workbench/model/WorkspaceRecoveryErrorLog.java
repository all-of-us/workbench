package org.pmiops.workbench.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class WorkspaceRecoveryErrorLog implements Serializable {

  @JsonProperty("id")
  private long id;

  @JsonProperty("workspaceId")
  private long workspaceId;

  @JsonProperty("errorMessage")
  private String errorMessage;

  @JsonProperty("errorType")
  private String errorType;

  @JsonProperty("stackTrace")
  private String stackTrace;

  @JsonProperty("createdTime")
  private String createdTime;

  public WorkspaceRecoveryErrorLog() {}

  public WorkspaceRecoveryErrorLog(
      long id,
      long workspaceId,
      String errorMessage,
      String errorType,
      String stackTrace,
      String createdTime) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.errorMessage = errorMessage;
    this.errorType = errorType;
    this.stackTrace = stackTrace;
    this.createdTime = createdTime;
  }

  public long getId() {
    return id;
  }

  public WorkspaceRecoveryErrorLog setId(long id) {
    this.id = id;
    return this;
  }

  public long getWorkspaceId() {
    return workspaceId;
  }

  public WorkspaceRecoveryErrorLog setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public WorkspaceRecoveryErrorLog setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public String getErrorType() {
    return errorType;
  }

  public WorkspaceRecoveryErrorLog setErrorType(String errorType) {
    this.errorType = errorType;
    return this;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public WorkspaceRecoveryErrorLog setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getCreatedTime() {
    return createdTime;
  }

  public WorkspaceRecoveryErrorLog setCreatedTime(String createdTime) {
    this.createdTime = createdTime;
    return this;
  }

  @Override
  public String toString() {
    return "WorkspaceRecoveryErrorLog{"
        + "id="
        + id
        + ", workspaceId="
        + workspaceId
        + ", errorMessage='"
        + errorMessage
        + '\''
        + ", errorType='"
        + errorType
        + '\''
        + ", createdTime='"
        + createdTime
        + '\''
        + '}';
  }
}

