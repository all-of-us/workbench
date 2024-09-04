package org.pmiops.workbench.db.model;

import com.google.common.base.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.pmiops.workbench.model.OrganizationType;

@Entity
@Table(name = "institution")
public class DbInstitution {

  private long institutionId; // primary opaque key for DB use only
  private String shortName; // unique key exposed to API
  private String displayName;
  private Short organizationTypeEnum;
  private String organizationTypeOtherText;
  private String requestAccessUrl;
  private boolean bypassInitialCreditsExpiration;

  public DbInstitution() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_id")
  public long getInstitutionId() {
    return institutionId;
  }

  public DbInstitution setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  @Column(name = "short_name", nullable = false, unique = true)
  public String getShortName() {
    return shortName;
  }

  public DbInstitution setShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  @Column(name = "display_name", nullable = false, unique = true)
  public String getDisplayName() {
    return displayName;
  }

  public DbInstitution setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  @Column(name = "organization_type_enum")
  public OrganizationType getOrganizationTypeEnum() {
    return DbStorageEnums.organizationTypeFromStorage(organizationTypeEnum);
  }

  public DbInstitution setOrganizationTypeEnum(OrganizationType type) {
    this.organizationTypeEnum = DbStorageEnums.organizationTypeToStorage(type);
    return this;
  }

  @Column(name = "organization_type_other_text")
  public String getOrganizationTypeOtherText() {
    return organizationTypeOtherText;
  }

  public DbInstitution setOrganizationTypeOtherText(String organizationTypeOtherText) {
    this.organizationTypeOtherText = organizationTypeOtherText;
    return this;
  }

  @Column(name = "request_access_url")
  public String getRequestAccessUrl() {
    return requestAccessUrl;
  }

  public DbInstitution setRequestAccessUrl(String requestAccessUrl) {
    this.requestAccessUrl = requestAccessUrl;
    return this;
  }

  @Column(name = "bypass_initial_credits_expiration")
  public boolean getBypassInitialCreditsExpiration() {
    return bypassInitialCreditsExpiration;
  }

  public DbInstitution setBypassInitialCreditsExpiration(boolean bypassInitialCreditsExpiration) {
    this.bypassInitialCreditsExpiration = bypassInitialCreditsExpiration;
    return this;
  }

  // omit ID field from equality so equivalent objects match regardless
  // of whether they are actually present in the DB

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbInstitution that = (DbInstitution) o;

    return Objects.equal(shortName, that.shortName)
        && Objects.equal(displayName, that.displayName)
        && Objects.equal(organizationTypeEnum, that.organizationTypeEnum)
        && Objects.equal(organizationTypeOtherText, that.organizationTypeOtherText)
        && Objects.equal(requestAccessUrl, that.requestAccessUrl)
        && Objects.equal(bypassInitialCreditsExpiration, that.bypassInitialCreditsExpiration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        shortName,
        displayName,
        organizationTypeEnum,
        organizationTypeOtherText,
        requestAccessUrl,
        bypassInitialCreditsExpiration);
  }
}
