package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "access_module")
public class DbAccessModule {
  private long accessModuleId;
  private boolean expirable;
  private AccessModuleName name;

  public DbAccessModule() {}

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
  public AccessModuleName getName() {
    return name;
  }

  public DbAccessModule setName(AccessModuleName name) {
    this.name = name;
    return this;
  }

  public enum AccessModuleName {
    ERA_COMMONS,
    TWO_FACTOR_AUTH,
    RAS_LOGIN_GOV,
    RT_COMPLIANCE_TRAINING,
    DATA_USER_CODE_OF_CONDUCT,
    PROFILE_CONFIRMATION,
    PUBLICATION_CONFIRMATION,
  }
}
