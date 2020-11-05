package org.pmiops.workbench.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;

@Embeddable
public class DbAccessModuleMemberId implements Serializable {
  private long accessPolicyId;
  private long accessModuleId;

  public DbAccessModuleMemberId(long accessPolicyId, long accessModuleId) {
    this.accessPolicyId = accessPolicyId;
    this.accessModuleId = accessModuleId;
  }

  public DbAccessModuleMemberId() {}

  public long getAccessPolicyId() {
    return accessPolicyId;
  }

  public void setAccessPolicyId(long accessPolicyId) {
    this.accessPolicyId = accessPolicyId;
  }

  public long getAccessModuleId() {
    return accessModuleId;
  }

  public void setAccessModuleId(long accessModuleId) {
    this.accessModuleId = accessModuleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbAccessModuleMemberId)) {
      return false;
    }
    DbAccessModuleMemberId that = (DbAccessModuleMemberId) o;
    return accessPolicyId == that.accessPolicyId && accessModuleId == that.accessModuleId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessPolicyId, accessModuleId);
  }

  @Override
  public String toString() {
    return "DbAccessModuleMemberId{"
        + "accessPolicyId="
        + accessPolicyId
        + ", accessModuleId="
        + accessModuleId
        + '}';
  }
}
