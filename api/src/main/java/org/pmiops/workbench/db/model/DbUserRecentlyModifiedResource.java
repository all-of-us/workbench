package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_recently_modified_resource")
public class DbUserRecentlyModifiedResource {
  public enum DbUserRecentlyModifiedResourceType {
    COHORT,
    CONCEPT_SET,
    NOTEBOOK,
    DATA_SET,
    COHORT_REVIEW
  }

  private int id;
  private Long userId;
  private Long workspaceId;
  private DbUserRecentlyModifiedResourceType resourceTypeEnum;
  private String resourceId;
  private Timestamp lastAccessDate;

  public DbUserRecentlyModifiedResource() {}

  public DbUserRecentlyModifiedResource(
      long workspaceId,
      long userId,
      DbUserRecentlyModifiedResourceType resourceTypeEnum,
      String resourceId,
      Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.resourceId = resourceId;
    this.resourceTypeEnum = resourceTypeEnum;
    this.lastAccessDate = lastAccessDate;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public DbUserRecentlyModifiedResource setId(int id) {
    this.id = id;
    return this;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public DbUserRecentlyModifiedResource setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbUserRecentlyModifiedResource setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false)
  public DbUserRecentlyModifiedResourceType getResourceType() {
    return resourceTypeEnum;
  }

  public DbUserRecentlyModifiedResource setResourceType(DbUserRecentlyModifiedResourceType s) {
    this.resourceTypeEnum = s;
    return this;
  }

  @Column(name = "resource_id")
  public String getResourceId() {
    return resourceId;
  }

  public DbUserRecentlyModifiedResource setResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  @Column(name = "last_access_date")
  public Timestamp getLastAccessDate() {
    return lastAccessDate;
  }

  public DbUserRecentlyModifiedResource setLastAccessDate(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
    return this;
  }
}
