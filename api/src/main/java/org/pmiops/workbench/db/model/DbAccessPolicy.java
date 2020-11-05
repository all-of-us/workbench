package org.pmiops.workbench.db.model;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.pmiops.workbench.model.AccessModule;

@Entity
public class DbAccessPolicy {

  private long accessPolicyId;
  private String displayName;
  private Set<DbAccessModule> accessModules;

  public DbAccessPolicy() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "access_policy_id")
  long getAccessPolicyId() {
    return accessPolicyId;
  }

  public void setAccessPolicyId(long accessPolicyId) {
    this.accessPolicyId = accessPolicyId;
  }

  @Column(name = "display_name")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @OneToMany(mappedBy = "accessModuleId")
  public Set<DbAccessModule> getAccessModules() {
    return accessModules;
  }

  public void setAccessModules(Set<DbAccessModule> accessModules) {
    this.accessModules = accessModules;
  }
}
