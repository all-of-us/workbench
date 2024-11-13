package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_initial_credits_expiration")
public class DbUserInitialCreditsExpiration {

  private long id;
  private DbUser user;
  private Timestamp creditStartTime;
  private Timestamp expirationTime;
  private Timestamp approachingExpirationNotificationTime;
  private Timestamp expirationCleanupTime;
  private boolean bypassed;
  private Timestamp extensionDate;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbUserInitialCreditsExpiration setId(long id) {
    this.id = id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbUserInitialCreditsExpiration setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @Column(name = "credit_start_time")
  public Timestamp getCreditStartTime() {
    return creditStartTime;
  }

  public DbUserInitialCreditsExpiration setCreditStartTime(Timestamp creditStartTime) {
    this.creditStartTime = creditStartTime;
    return this;
  }

  @Column(name = "expiration_time")
  public Timestamp getExpirationTime() {
    return expirationTime;
  }

  public DbUserInitialCreditsExpiration setExpirationTime(Timestamp expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }

  @Column(name = "bypassed")
  public boolean isBypassed() {
    return bypassed;
  }

  public DbUserInitialCreditsExpiration setBypassed(boolean bypassed) {
    this.bypassed = bypassed;
    return this;
  }

  @Column(name = "extension_date")
  public Timestamp getExtensionDate() {
    return extensionDate;
  }

  public DbUserInitialCreditsExpiration setExtensionDate(Timestamp extensionDate) {
    this.extensionDate = extensionDate;
    return this;
  }

  @Column(name = "approaching_expiration_notification_time")
  public Timestamp getApproachingExpirationNotificationTime() {
    return approachingExpirationNotificationTime;
  }

  public DbUserInitialCreditsExpiration setApproachingExpirationNotificationTime(
      Timestamp approachingExpirationNotificationTime) {
    this.approachingExpirationNotificationTime = approachingExpirationNotificationTime;
    return this;
  }

  @Column(name = "expiration_cleanup_time")
  public Timestamp getExpirationCleanupTime() {
    return expirationCleanupTime;
  }

  public DbUserInitialCreditsExpiration setExpirationCleanupTime(Timestamp expirationCleanupTime) {
    this.expirationCleanupTime = expirationCleanupTime;
    return this;
  }
}
