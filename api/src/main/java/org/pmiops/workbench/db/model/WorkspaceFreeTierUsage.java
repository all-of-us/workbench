package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "workspace_free_tier_usage")
public class WorkspaceFreeTierUsage {
  private long id;
  private long userId;
  private long workspaceId;
  private double cost;
  private Timestamp lastUpdateTime;

  public WorkspaceFreeTierUsage(Workspace workspace) {
    this.userId = workspace.getCreator().getUserId();
    this.workspaceId = workspace.getWorkspaceId();
    setLastUpdateTime();
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

  @Column(name = "cost")
  public double getCost() {
    return cost;
  }

  public void setCost(double cost) {
    this.cost = cost;
  }

  @Column(name = "last_update_time")
  public Timestamp getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(Timestamp lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public void setLastUpdateTime() {
    this.lastUpdateTime = new Timestamp(Instant.now().toEpochMilli());
  }
}
