package org.pmiops.workbench.db.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "workspace_recovery_error_log",
    indexes = {
      @Index(name = "idx_workspace_recovery_error_log_workspace_id", columnList = "workspace_id"),
      @Index(name = "idx_workspace_recovery_error_log_created_time", columnList = "created_time")
    })
public class DbWorkspaceRecoveryErrorLog {

  private long id;
  private long workspaceId;
  private String errorMessage;
  private String errorType;
  private String stackTrace;
  private Timestamp createdTime;

  public DbWorkspaceRecoveryErrorLog() {}

  public DbWorkspaceRecoveryErrorLog(
      long workspaceId, String errorMessage, String errorType, String stackTrace) {
    this.workspaceId = workspaceId;
    this.errorMessage = errorMessage;
    this.errorType = errorType;
    this.stackTrace = stackTrace;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbWorkspaceRecoveryErrorLog setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "workspace_id", nullable = false)
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbWorkspaceRecoveryErrorLog setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
  public String getErrorMessage() {
    return errorMessage;
  }

  public DbWorkspaceRecoveryErrorLog setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  @Column(name = "error_type", nullable = true)
  public String getErrorType() {
    return errorType;
  }

  public DbWorkspaceRecoveryErrorLog setErrorType(String errorType) {
    this.errorType = errorType;
    return this;
  }

  @Column(name = "stack_trace", nullable = true, columnDefinition = "TEXT")
  public String getStackTrace() {
    return stackTrace;
  }

  public DbWorkspaceRecoveryErrorLog setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  @CreationTimestamp
  @Column(name = "created_time", nullable = false, updatable = false)
  public Timestamp getCreatedTime() {
    return createdTime;
  }

  public DbWorkspaceRecoveryErrorLog setCreatedTime(Timestamp createdTime) {
    this.createdTime = createdTime;
    return this;
  }
}

