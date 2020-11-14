package org.pmiops.workbench.db.model.embedded;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;

/**
 * Composite primary key for access_policy_module table
 */
@Embeddable
public class DbAccessPolicyModuleKey implements Serializable {
  private Long accessModuleId;
  private Long accessPolicyId;

  public DbAccessPolicyModuleKey() {
  }

  public DbAccessPolicyModuleKey(Long accessModuleId, Long accessPolicyId) {
    this.accessModuleId = accessModuleId;
    this.accessPolicyId = accessPolicyId;
  }

  public Long getAccessModuleId() {
    return accessModuleId;
  }

  public void setAccessModuleId(Long accessModuleId) {
    this.accessModuleId = accessModuleId;
  }

  public Long getAccessPolicyId() {
    return accessPolicyId;
  }
  public void setAccessPolicyId(Long accessPolicyId) {
    this.accessPolicyId = accessPolicyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbAccessPolicyModuleKey)) {
      return false;
    }
    DbAccessPolicyModuleKey that = (DbAccessPolicyModuleKey) o;
    return Objects.equals(accessModuleId, that.accessModuleId) &&
        Objects.equals(accessPolicyId, that.accessPolicyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessModuleId, accessPolicyId);
  }
}
