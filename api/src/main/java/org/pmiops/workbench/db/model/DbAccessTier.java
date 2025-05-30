package org.pmiops.workbench.db.model;

import com.google.common.base.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_tier")
public class DbAccessTier {

  private long accessTierId; // primary opaque key for DB use only
  private String shortName; // unique key exposed to API
  private String displayName;
  private String servicePerimeter;
  private String authDomainName;
  private String authDomainGroupEmail;
  private String datasetsBucket;
  private Boolean enableUserWorkflows;
  private String vwbTierGroupName;

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

  @Column(name = "auth_domain_group_email", nullable = false)
  public String getAuthDomainGroupEmail() {
    return authDomainGroupEmail;
  }

  public DbAccessTier setAuthDomainGroupEmail(String authDomainGroupEmail) {
    this.authDomainGroupEmail = authDomainGroupEmail;
    return this;
  }

  @Column(name = "datasets_bucket")
  public String getDatasetsBucket() {
    return datasetsBucket;
  }

  public DbAccessTier setDatasetsBucket(String datasetsBucket) {
    this.datasetsBucket = datasetsBucket;
    return this;
  }

  @Column(name = "enable_user_workflows")
  public Boolean getEnableUserWorkflows() {
    return enableUserWorkflows;
  }

  public DbAccessTier setEnableUserWorkflows(Boolean enableUserWorkflows) {
    this.enableUserWorkflows = enableUserWorkflows;
    return this;
  }

  @Column(name = "vwb_tier_group_name")
  public String getVwbTierGroupName() {
    return vwbTierGroupName;
  }

  public DbAccessTier setVwbTierGroupName(String vwbTierGroupName) {
    this.vwbTierGroupName = vwbTierGroupName;
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
        && Objects.equal(authDomainGroupEmail, that.authDomainGroupEmail)
        && Objects.equal(datasetsBucket, that.datasetsBucket)
        && Objects.equal(enableUserWorkflows, that.enableUserWorkflows)
        && Objects.equal(vwbTierGroupName, that.vwbTierGroupName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        shortName,
        displayName,
        servicePerimeter,
        authDomainName,
        authDomainGroupEmail,
        datasetsBucket,
        vwbTierGroupName);
  }
}
