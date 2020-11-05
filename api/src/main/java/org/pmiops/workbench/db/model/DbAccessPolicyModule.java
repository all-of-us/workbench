package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Membership list for AccessModules in AccessPolicies
 */
@IdClass(DbAccessModuleMemberId.class)
@Table(name = "access_policy_module")
public class DbAccessPolicyModule {

  @Id private long accessPolicyId;
  @Id private long accessModuleId;

  @ManyToMany(mappedBy = "access_policies")

  @Column(name="access_policy_id")
  public void setAccessPolicyId(long accessPolicyId) {
    this.accessPolicyId = accessPolicyId;
  }

  public long getAccessModuleId() {
    return accessModuleId;
  }

  @Column(name="access_module_id")
  public void setAccessModuleId(long accessModuleId) {
    this.accessModuleId = accessModuleId;
  }

  public long getAccessPolicyId() {
    return accessPolicyId;
  }

  private DbAccessModuleMemberId dbAccessModuleMemberId;

  public DbAccessPolicyModule() {
  }

  public DbAccessModuleMemberId getDbAccessModuleMemberId() {
    return dbAccessModuleMemberId;
  }

  public void setDbAccessModuleMemberId(
      DbAccessModuleMemberId dbAccessModuleMemberId) {
    this.dbAccessModuleMemberId = dbAccessModuleMemberId;
  }
}
