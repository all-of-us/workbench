package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "vwb_sa_cleanup")
public class DbVwbSaCleanup {

  public enum CleanupState {
    IN_PROGRESS,
    FAILED,
    REMOVED
  }

  private long id;
  private String workspaceId;
  private Timestamp started;
  private String status;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbVwbSaCleanup setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "workspace_id", nullable = false)
  public String getWorkspaceId() {
    return workspaceId;
  }

  public DbVwbSaCleanup setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Column(name = "started")
  public Timestamp getStarted() {
    return started;
  }

  public DbVwbSaCleanup setStarted(Timestamp started) {
    this.started = started;
    return this;
  }

  @Column(name = "status")
  public String getStatus() {
    return status;
  }

  public DbVwbSaCleanup setStatus(String status) {
    this.status = status;
    return this;
  }
}
