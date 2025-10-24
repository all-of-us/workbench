package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_egress_bypass_window")
@EntityListeners(AuditingEntityListener.class)
public class DbUserEgressBypassWindow {
  private long egressBypassId;
  private long userId;

  private Timestamp startTime;
  private Timestamp endTime;

  private String description;
  private String vwbWorkspaceUfid;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "egress_bypass_id", nullable = false)
  public long getEgressBypassId() {
    return egressBypassId;
  }

  public DbUserEgressBypassWindow setEgressBypassId(long egressBypassId) {
    this.egressBypassId = egressBypassId;
    return this;
  }

  @Column(name = "user_id", nullable = false)
  public long getUserId() {
    return userId;
  }

  public DbUserEgressBypassWindow setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @Column(name = "start_time", nullable = false)
  public Timestamp getStartTime() {
    return startTime;
  }

  public DbUserEgressBypassWindow setStartTime(Timestamp startTime) {
    this.startTime = startTime;
    return this;
  }

  @Column(name = "end_time", nullable = false)
  public Timestamp getEndTime() {
    return endTime;
  }

  public DbUserEgressBypassWindow setEndTime(Timestamp endTime) {
    this.endTime = endTime;
    return this;
  }

  @Column(name = "description", nullable = false)
  public String getDescription() {
    return description;
  }

  public DbUserEgressBypassWindow setDescription(String description) {
    this.description = description;
    return this;
  }

  @Column(name = "vwb_workspace_ufid")
  public String getVwbWorkspaceUfid() {
    return vwbWorkspaceUfid;
  }

  public DbUserEgressBypassWindow setVwbWorkspaceUfid(String vwbWorkspaceUfid) {
    this.vwbWorkspaceUfid = vwbWorkspaceUfid;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DbUserEgressBypassWindow)) return false;

    DbUserEgressBypassWindow dbUserEgressBypassWindow = (DbUserEgressBypassWindow) o;

    return new EqualsBuilder()
        .append(userId, dbUserEgressBypassWindow.userId)
        .append(egressBypassId, dbUserEgressBypassWindow.egressBypassId)
        .append(startTime, dbUserEgressBypassWindow.startTime)
        .append(endTime, dbUserEgressBypassWindow.endTime)
        .append(description, dbUserEgressBypassWindow.description)
        .append(vwbWorkspaceUfid, dbUserEgressBypassWindow.vwbWorkspaceUfid)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(userId)
        .append(startTime)
        .append(endTime)
        .append(egressBypassId)
        .append(description)
        .append(vwbWorkspaceUfid)
        .toHashCode();
  }
}
