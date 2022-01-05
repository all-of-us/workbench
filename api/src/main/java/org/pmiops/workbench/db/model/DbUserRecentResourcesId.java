package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.ResourceType;

@Entity
@Table(name = "user_recent_resources")
public class DbUserRecentResourcesId {
  private int id;
  private Long userId;
  private Long workspaceId;
  private short resourceType;
  private ResourceType resourceTypeEnum;
  private String resourceId;
  private Timestamp lastAccessDate;

  public DbUserRecentResourcesId() {}

  public DbUserRecentResourcesId(
      long workspaceId,
      long userId,
      ResourceType resourceTypeEnum,
      String resourceId,
      Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.resourceId = resourceId;
    this.resourceTypeEnum = resourceTypeEnum;
    setResourceTypeEnum(resourceTypeEnum);
    this.lastAccessDate = lastAccessDate;
  }

  public DbUserRecentResourcesId(
      long workspaceId,
      long userId,
      short resourceType,
      String resourceId,
      Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.lastAccessDate = lastAccessDate;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Column(name = "resource_type")
  public short getResourceType() {
    return resourceType;
  }

  public void setResourceType(short resourceType) {
    this.resourceType = resourceType;
  }

  @Transient
  public ResourceType getResourceTypeEnum() {
    return DbStorageEnums.resourceTypeFromStorage(getResourceType());
  }

  public void setResourceTypeEnum(ResourceType resourceTypeEnum) {
    this.resourceTypeEnum = resourceTypeEnum;
    setResourceType(DbStorageEnums.resourceTypeToStorage(resourceTypeEnum));
  }

  @Column(name = "resource_id")
  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  @Column(name = "last_access_date")
  public Timestamp getLastAccessDate() {
    return lastAccessDate;
  }

  public void setLastAccessDate(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }
}
