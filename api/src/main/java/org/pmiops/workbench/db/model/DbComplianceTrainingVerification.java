package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
