package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "folder_sync_transfer")
public class DbFolderSyncTransfer {

  public enum TransferState {
    NOT_STARTED,
    IN_PROGRESS,
    FAILED,
    FINISHED
  }

  private long id;
  private long createdByUserId;
  private Timestamp started;
  private Timestamp finished;
  private String transferJobName;
  private String transferState;
  private String sourceWorkspaceNamespace;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbFolderSyncTransfer setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "created_by_user_id", nullable = false)
  public long getCreatedByUserId() {
    return createdByUserId;
  }

  public DbFolderSyncTransfer setCreatedByUserId(long createdByUserId) {
    this.createdByUserId = createdByUserId;
    return this;
  }

  @Column(name = "started")
  public Timestamp getStarted() {
    return started;
  }

  public DbFolderSyncTransfer setStarted(Timestamp started) {
    this.started = started;
    return this;
  }

  @Column(name = "finished")
  public Timestamp getFinished() {
    return finished;
  }

  public DbFolderSyncTransfer setFinished(Timestamp finished) {
    this.finished = finished;
    return this;
  }

  @Column(name = "transfer_job_name")
  public String getTransferJobName() {
    return transferJobName;
  }

  public DbFolderSyncTransfer setTransferJobName(String transferJobName) {
    this.transferJobName = transferJobName;
    return this;
  }

  @Column(name = "transfer_state")
  public String getTransferState() {
    return transferState;
  }

  public DbFolderSyncTransfer setTransferState(String transferState) {
    this.transferState = transferState;
    return this;
  }

  @Column(name = "source_workspace_namespace")
  public String getSourceWorkspaceNamespace() {
    return sourceWorkspaceNamespace;
  }

  public DbFolderSyncTransfer setSourceWorkspaceNamespace(String sourceWorkspaceNamespace) {
    this.sourceWorkspaceNamespace = sourceWorkspaceNamespace;
    return this;
  }
}
