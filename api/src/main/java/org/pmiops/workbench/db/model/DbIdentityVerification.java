package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_verification")
public class DbIdentityVerification {
  private long identityVerificationId;
  private DbUser user;
  private DbIdentityVerificationSystem identityVerificationSystem;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "identity_verification_id")
  public long getIdentityVerificationId() {
    return identityVerificationId;
  }

  public DbIdentityVerification setIdentityVerificationId(long identityVerificationId) {
    this.identityVerificationId = identityVerificationId;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false)
  public DbUser getUser() {
    return user;
  }

  public DbIdentityVerification setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "identity_verification_system", nullable = false)
  public DbIdentityVerificationSystem getIdentityVerificationSystem() {
    return identityVerificationSystem;
  }

  public DbIdentityVerification setIdentityVerificationSystem(
      DbIdentityVerificationSystem identityVerificationSystem) {
    this.identityVerificationSystem = identityVerificationSystem;
    return this;
  }

  public enum DbIdentityVerificationSystem {
    ID_ME,
    LOGIN_GOV,
  }
}
