package org.pmiops.workbench.db.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "access_policy",
    uniqueConstraints = {@UniqueConstraint(columnNames = "display_name")})
public class DbAccessPolicy implements Serializable {

  private long accessPolicyId;
  private String displayName;
  private final Set<DbAccessModule> accessModules = new HashSet<>();

  public DbAccessPolicy() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "access_policy_id")
  public long getAccessPolicyId() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbAccessPolicy)) {
      return false;
    }
    DbAccessPolicy that = (DbAccessPolicy) o;
    return accessPolicyId == that.accessPolicyId
        && Objects.equals(displayName, that.displayName)
        && Objects.equals(accessModules, that.accessModules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessPolicyId, displayName, accessModules);
  }

  // DbAccessPolicy is the owning side of the many:many relationship, and is
  // responsible for maintaining the integrity of the join table access_module_policy.
  @ManyToMany(cascade = CascadeType.ALL) // FIXME!!
  @JoinTable(
      name = "access_module_policy",
      joinColumns = @JoinColumn(name = "access_policy_id"),
      inverseJoinColumns = @JoinColumn(name = "access_module_id"))
  public Set<DbAccessModule> getAccessModules() {
    return accessModules;
  }

  public void setAccessModules(Set<DbAccessModule> accessModules) {
    this.accessModules.clear();
    this.accessModules.addAll(accessModules);
  }
}
