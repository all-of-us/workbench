package org.pmiops.workbench.db.model;

import com.google.common.base.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "access_tier")
public class DbAccessTier {

  private long accessTierId; // primary opaque key for DB use only
  private String shortName; // unique key exposed to API
  private String displayName;
  private String servicePerimeter;
  private String authDomainName;

  public DbAccessTier() {}

  @Id
  @Column(name = "access_tier_id")
  public long getAccessTierId() {
    return accessTierId;
  }

  public DbAccessTier setAccessTierId(long accessTierId) {
    this.accessTierId = accessTierId;
    return this;
  }

  @Column(name = "short_name", nullable = false, unique = true)
  public String getShortName() {
    return shortName;
  }

  public DbAccessTier setShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  @Column(name = "display_name", nullable = false, unique = true)
  public String getDisplayName() {
    return displayName;
  }

  public DbAccessTier setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  @Column(name = "service_perimeter", nullable = false)
  public String getServicePerimeter() {
    return servicePerimeter;
  }

  public DbAccessTier setServicePerimeter(String servicePerimeter) {
    this.servicePerimeter = servicePerimeter;
    return this;
  }

  @Column(name = "auth_domain_name", nullable = false)
  public String getAuthDomainName() {
    return authDomainName;
  }

  public DbAccessTier setAuthDomainName(String authDomainName) {
    this.authDomainName = authDomainName;
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
    DbAccessTier that = (DbAccessTier) o;
    return Objects.equal(shortName, that.shortName)
        && Objects.equal(displayName, that.displayName)
        && Objects.equal(servicePerimeter, that.servicePerimeter)
        && Objects.equal(authDomainName, that.authDomainName)
        && Objects.equal(authDomainGroupEmail, that.authDomainGroupEmail);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        shortName, displayName, servicePerimeter, authDomainName, authDomainGroupEmail);
  }
}
