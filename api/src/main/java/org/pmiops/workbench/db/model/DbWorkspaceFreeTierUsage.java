package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "workspace_free_tier_usage")
public class DbWorkspaceFreeTierUsage {
  private long id;
  private DbUser user;
  private DbWorkspace workspace;
  private double cost;
  private Timestamp lastUpdateTime;

  public DbWorkspaceFreeTierUsage() {}

  public DbWorkspaceFreeTierUsage(DbWorkspace workspace) {
    this.user = workspace.getCreator();
    this.workspace = workspace;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workspace_free_tier_usage_id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser user) {
    this.user = user;
  }

  @OneToOne
  @JoinColumn(name = "workspace_id")
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
  }

  @Column(name = "cost")
  public double getCost() {
    return cost;
  }

  public void setCost(double cost) {
    this.cost = cost;
    setLastUpdateTime();
  }

  @Column(name = "last_update_time")
  public Timestamp getLastUpdateTime() {
    return lastUpdateTime;
  }

  private void setLastUpdateTime(Timestamp lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  private void setLastUpdateTime() {
    this.lastUpdateTime = new Timestamp(Instant.now().toEpochMilli());
  }
}
