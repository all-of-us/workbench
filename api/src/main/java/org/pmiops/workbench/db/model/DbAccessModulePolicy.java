package org.pmiops.workbench.db.model;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import org.pmiops.workbench.db.model.embedded.DbAccessPolicyModuleKey;

@Entity
@Table(name = "access_module_policy")
public class DbAccessModulePolicy {

  private DbAccessPolicyModuleKey accessPolicyModuleKey;
  private DbAccessModule accessModule;
  private DbAccessPolicy accessPolicy;

  public DbAccessModulePolicy() {
  }

  @EmbeddedId
  public DbAccessPolicyModuleKey getAccessPolicyModuleKey() {
    return accessPolicyModuleKey;
  }

  public void setAccessPolicyModuleKey(
      DbAccessPolicyModuleKey accessPolicyModuleKey) {
    this.accessPolicyModuleKey = accessPolicyModuleKey;
  }

  @ManyToOne
  @MapsId("accessModuleId")
  @JoinColumn(name = "access_module_id")
  public DbAccessModule getAccessModule() {
    return accessModule;
  }

  public void setAccessModule(DbAccessModule accessModule) {
    this.accessModule = accessModule;
  }

  @ManyToOne
  @MapsId("accessPolicyId")
  @JoinColumn(name = "access_policy_id")
  public DbAccessPolicy getAccessPolicy() {
    return accessPolicy;
  }

  public void setAccessPolicy(DbAccessPolicy accessPolicy) {
    this.accessPolicy = accessPolicy;
  }
}
