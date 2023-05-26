package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_egress_bypass_window")
@EntityListeners(AuditingEntityListener.class)
public class DbUserEgressBypassWindow {
  private long egressBypassId;
  private DbUser user;

  private Timestamp startTime;
  private Timestamp endTime;

  private String description;

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

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbUserEgressBypassWindow setUser(DbUser user) {
    this.user = user;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DbUserEgressBypassWindow)) return false;

    DbUserEgressBypassWindow dbUserEgressBypassWindow = (DbUserEgressBypassWindow) o;

    return new EqualsBuilder()
        .append(user, dbUserEgressBypassWindow.user)
        .append(egressBypassId, dbUserEgressBypassWindow.egressBypassId)
        .append(startTime, dbUserEgressBypassWindow.startTime)
        .append(endTime, dbUserEgressBypassWindow.endTime)
        .append(description, dbUserEgressBypassWindow.description)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(user)
        .append(startTime)
        .append(endTime)
        .append(egressBypassId)
        .append(description)
        .toHashCode();
  }
}
