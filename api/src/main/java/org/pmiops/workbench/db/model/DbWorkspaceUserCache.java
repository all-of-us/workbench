package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "workspace_user_cache")
public class DbWorkspaceUserCache {
  private long id;
  private long workspaceId;
  private long userId;
  private String role;
  private Timestamp lastUpdated;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long getId() {
    return id;
  }

  public DbWorkspaceUserCache setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbWorkspaceUserCache setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public DbWorkspaceUserCache setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @Column(name = "role")
  public String getRole() {
    return role;
  }

  public DbWorkspaceUserCache setRole(String role) {
    this.role = role;
    return this;
  }

  @Column(name = "last_updated")
  public Timestamp getLastUpdated() {
    return lastUpdated;
  }

  public DbWorkspaceUserCache setLastUpdated(Timestamp lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbWorkspaceUserCache that = (DbWorkspaceUserCache) o;
    return id == that.id
        && workspaceId == that.workspaceId
        && userId == that.userId
        && Objects.equals(role, that.role)
        && Objects.equals(lastUpdated, that.lastUpdated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, workspaceId, userId, role, lastUpdated);
  }

  public DbWorkspaceUserCache() {}

  public DbWorkspaceUserCache(long workspaceId, long userId, String role, Timestamp lastUpdated) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.role = role;
    this.lastUpdated = lastUpdated;
  }
}
