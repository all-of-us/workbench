package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "workspace_operation")
@EntityListeners(AuditingEntityListener.class)
public class DbWorkspaceOperation {
  public enum DbWorkspaceOperationStatus {
    PENDING,
    ERROR,
    SUCCESS
  }

  private long id;
  private long creatorId;
  private DbWorkspaceOperationStatus status;
  private Long workspaceId;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  public long getId() {
    return id;
  }

  public DbWorkspaceOperation setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "creator_id", nullable = false)
  public long getCreatorId() {
    return creatorId;
  }

  public DbWorkspaceOperation setCreatorId(long creatorId) {
    this.creatorId = creatorId;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public DbWorkspaceOperationStatus getStatus() {
    return status;
  }

  public DbWorkspaceOperation setStatus(DbWorkspaceOperationStatus status) {
    this.status = status;
    return this;
  }

  @Column(name = "workspace_id")
  public Long getWorkspaceId() {
    return workspaceId;
  }

  public DbWorkspaceOperation setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @CreatedDate
  @Column(name = "creation_time", nullable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbWorkspaceOperation setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @LastModifiedDate
  @Column(name = "last_modified_time", nullable = false)
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public DbWorkspaceOperation setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbWorkspaceOperation that = (DbWorkspaceOperation) o;
    return id == that.id
        && creatorId == that.creatorId
        && Objects.equals(workspaceId, that.workspaceId)
        && creationTime.equals(that.creationTime)
        && lastModifiedTime.equals(that.lastModifiedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, creatorId, workspaceId, creationTime, lastModifiedTime);
  }
}
