package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_workspace_app")
public class DbUserWorkspaceApp {
  private long id;
  private long userId;
  private long workspaceId;
  private String appName;
  private String appType;

  public DbUserWorkspaceApp() {}

  public DbUserWorkspaceApp(long userId, long workspaceId, String appName, String appType) {
    this.userId = userId;
    this.workspaceId = workspaceId;
    this.appName = appName;
    this.appType = appType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Column(name = "app_name")
  public String getAppName() {
    return appName;
  }

  public void setAppName(String cromwellAppName) {
    this.appName = cromwellAppName;
  }

  @Column(name = "app_type")
  public String getAppType() {
    return appType;
  }

  public void setAppType(String appType) {
    this.appType = appType;
  }
}
