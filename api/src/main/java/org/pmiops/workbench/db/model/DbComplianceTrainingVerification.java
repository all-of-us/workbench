package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "compliance_training_verification")
public class DbComplianceTrainingVerification {
  private long complianceTrainingVerificationId;
  private DbUserAccessModule userAccessModule;
  private DbComplianceTrainingVerificationSystem complianceTrainingVerificationSystem;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "compliance_training_verification_id")
  public long getComplianceTrainingVerificationId() {
    return complianceTrainingVerificationId;
  }

  public DbComplianceTrainingVerification setComplianceTrainingVerificationId(
      long userAccessModuleId) {
    this.complianceTrainingVerificationId = userAccessModuleId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "user_access_module_id", nullable = false)
  public DbUserAccessModule getUserAccessModule() {
    return userAccessModule;
  }

  public DbComplianceTrainingVerification setUserAccessModule(DbUserAccessModule accessModule) {
    this.userAccessModule = accessModule;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "compliance_training_verification_system", nullable = false)
  public DbComplianceTrainingVerificationSystem getComplianceTrainingVerificationSystem() {
    return complianceTrainingVerificationSystem;
  }

  public DbComplianceTrainingVerification setComplianceTrainingVerificationSystem(
      DbComplianceTrainingVerificationSystem identityVerificationSystem) {
    this.complianceTrainingVerificationSystem = identityVerificationSystem;
    return this;
  }

  public enum DbComplianceTrainingVerificationSystem {
    MOODLE,
    ABSORB,
  }
}
