package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "workspace_recovery_error_log")
public class DbWorkspaceRecoveryErrorLog {

  private long id;
  private long workspaceId;
  private String errorMessage;
  private String errorType;
  private Timestamp createdTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long getId() {
    return id;
  }

  public DbWorkspaceRecoveryErrorLog setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbWorkspaceRecoveryErrorLog setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Column(name = "error_message")
  public String getErrorMessage() {
    return errorMessage;
  }

  public DbWorkspaceRecoveryErrorLog setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  @Column(name = "error_type")
  public String getErrorType() {
    return errorType;
  }

  public DbWorkspaceRecoveryErrorLog setErrorType(String errorType) {
    this.errorType = errorType;
    return this;
  }

  @Column(name = "created_time")
  public Timestamp getCreatedTime() {
    return createdTime;
  }

  public DbWorkspaceRecoveryErrorLog setCreatedTime(Timestamp createdTime) {
    this.createdTime = createdTime;
    return this;
  }
}
