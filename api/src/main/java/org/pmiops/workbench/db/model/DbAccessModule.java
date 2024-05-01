package org.pmiops.workbench.db.model;

import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForControlledTierAccess;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForRegisteredTierAccess;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "access_module")
public class DbAccessModule {
  private long accessModuleId;
  private boolean expirable;
  private DbAccessModuleName name;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "access_module_id")
  public long getAccessModuleId() {
    return accessModuleId;
  }

  public DbAccessModule setAccessModuleId(long accessModuleId) {
    this.accessModuleId = accessModuleId;
    return this;
  }

  @Column(name = "expirable", nullable = false)
  public boolean getExpirable() {
    return expirable;
  }

  public DbAccessModule setExpirable(boolean expirable) {
    this.expirable = expirable;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "name", nullable = false)
  public DbAccessModuleName getName() {
    return name;
  }

  public DbAccessModule setName(DbAccessModuleName name) {
    this.name = name;
    return this;
  }

  @Transient // used by WorkbenchConfigMapper
  public boolean getRequiredForRTAccess() {
    return getRequiredModulesForRegisteredTierAccess().contains(name);
  }

  @Transient // used by WorkbenchConfigMapper
  public boolean getRequiredForCTAccess() {
    return getRequiredModulesForControlledTierAccess().contains(name);
  }

  public enum DbAccessModuleName {
    ERA_COMMONS,
    TWO_FACTOR_AUTH,
    IDENTITY,
    RAS_ID_ME,
    RAS_LOGIN_GOV,
    RT_COMPLIANCE_TRAINING,
    DATA_USER_CODE_OF_CONDUCT,
    PROFILE_CONFIRMATION,
    PUBLICATION_CONFIRMATION,
    CT_COMPLIANCE_TRAINING,
  }
}
