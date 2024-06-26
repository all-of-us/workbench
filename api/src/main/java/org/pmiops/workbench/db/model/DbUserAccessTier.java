package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import org.pmiops.workbench.model.TierAccessStatus;

@Entity
@Table(name = "user_access_tier")
public class DbUserAccessTier {

  private long userAccessTierId;
  private DbUser user;
  private DbAccessTier accessTier;
  private Short tierAccessStatus;
  private Timestamp firstEnabled;
  private Timestamp lastUpdated;

  public DbUserAccessTier() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_access_tier_id")
  public long getUserAccessTierId() {
    return userAccessTierId;
  }

  public DbUserAccessTier setUserAccessTierId(long userAccessTierId) {
    this.userAccessTierId = userAccessTierId;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "user_id", nullable = false)
  public DbUser getUser() {
    return user;
  }

  public DbUserAccessTier setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "access_tier_id", nullable = false)
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbUserAccessTier setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
    return this;
  }

  @Column(name = "access_status", nullable = false)
  public Short getTierAccessStatus() {
    return tierAccessStatus;
  }

  @Transient
  public TierAccessStatus getTierAccessStatusEnum() {
    return DbStorageEnums.tierAccessStatusFromStorage(getTierAccessStatus());
  }

  public DbUserAccessTier setTierAccessStatus(TierAccessStatus status) {
    return setTierAccessStatus(DbStorageEnums.tierAccessStatusToStorage(status));
  }

  public DbUserAccessTier setTierAccessStatus(Short tierAccessStatus) {
    this.tierAccessStatus = tierAccessStatus;
    return this;
  }

  @Column(name = "first_enabled", nullable = false)
  public Timestamp getFirstEnabled() {
    return firstEnabled;
  }

  public DbUserAccessTier setFirstEnabled(Timestamp firstEnabled) {
    this.firstEnabled = firstEnabled;
    return this;
  }

  @Column(name = "last_updated", nullable = false)
  public Timestamp getLastUpdated() {
    return lastUpdated;
  }

  public DbUserAccessTier setLastUpdated(Timestamp lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }
}
