package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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

  public DbIdentityVerification setIdentityVerificationSystem(DbIdentityVerificationSystem identityVerificationSystem) {
    this.identityVerificationSystem = identityVerificationSystem;
    return this;
  }

  public enum DbIdentityVerificationSystem {
    ID_ME,
    LOGIN_GOV,
  }
}
