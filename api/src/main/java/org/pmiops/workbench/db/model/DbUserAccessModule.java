package org.pmiops.workbench.db.model;

import com.google.common.base.Objects;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "user_access_module")
public class DbUserAccessModule {
  private long userAccessModuleId;
  private DbUser user;
  private DbAccessModule accessModule;
  private Timestamp completionTime;
  private Timestamp bypassTime;
  private long optimisticLockingVersion;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_access_module_id")
  public long getUserAccessModuleId() {
    return userAccessModuleId;
  }

  public DbUserAccessModule setUserAccessModuleId(long userAccessModuleId) {
    this.userAccessModuleId = userAccessModuleId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  public DbUser getUser() {
    return user;
  }

  public DbUserAccessModule setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "access_module_id", nullable = false)
  public DbAccessModule getAccessModule() {
    return accessModule;
  }

  public DbUserAccessModule setAccessModule(DbAccessModule accessModule) {
    this.accessModule = accessModule;
    return this;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public DbUserAccessModule setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  @Column(name = "bypass_time")
  public Timestamp getBypassTime() {
    return bypassTime;
  }

  public DbUserAccessModule setBypassTime(Timestamp bypassTime) {
    this.bypassTime = bypassTime;
    return this;
  }

  @Version
  @Column(name = "version")
  public long getOptimisticLockingVersion() {
    return optimisticLockingVersion;
  }

  public DbUserAccessModule setOptimisticLockingVersion(long optimisticLockingVersion) {
    this.optimisticLockingVersion = optimisticLockingVersion;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbUserAccessModule that = (DbUserAccessModule) o;
    return Objects.equal(user, that.user)
        && Objects.equal(accessModule, that.accessModule)
        && Objects.equal(completionTime, that.completionTime)
        && Objects.equal(bypassTime, that.bypassTime);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(user, accessModule, completionTime, bypassTime);
  }

  @Override
  public String toString() {
    return "DbUserAccessModule{"
        + "userAccessModuleId="
        + userAccessModuleId
        + ", userId="
        + user.getUserId()
        + ", accessModuleId="
        + accessModule.getAccessModuleId()
        + ", completionTime="
        + completionTime
        + ", bypassTime="
        + bypassTime
        + ", optimisticLockingVersion="
        + optimisticLockingVersion
        + '}';
  }
}
