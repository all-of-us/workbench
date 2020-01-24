package org.pmiops.workbench.db.model;

import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "institution")
public class DbInstitution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_id")
  private long institutionId; // primary opaque key for DB use only

  @Column(name = "api_id", nullable = false, unique = true)
  private String apiId; // unique key exposed to API

  @Column(name = "long_name", nullable = false)
  private String longName;

  @Column(name = "organization_type_enum")
  private Short organizationTypeEnum;

  @Column(name = "organization_type_other_text")
  private String organizationTypeOtherText;

  @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
  private List<DbInstitutionEmailDomain> emailDomains;

  @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
  private List<DbInstitutionEmailAddress> emailAddresses;

  public DbInstitution() {}

  public DbInstitution(final String apiId, final String longName) {
    setApiId(apiId);
    setLongName(longName);
  }

  public long getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
  }

  public String getApiId() {
    return apiId;
  }

  public void setApiId(String apiId) {
    this.apiId = apiId;
  }

  public String getLongName() {
    return longName;
  }

  public void setLongName(String longName) {
    this.longName = longName;
  }

  public Short getOrganizationTypeEnum() {
    return organizationTypeEnum;
  }

  public void setOrganizationTypeEnum(Short organizationTypeEnum) {
    this.organizationTypeEnum = organizationTypeEnum;
  }

  public String getOrganizationTypeOtherText() {
    return organizationTypeOtherText;
  }

  public void setOrganizationTypeOtherText(String organizationTypeOtherText) {
    this.organizationTypeOtherText = organizationTypeOtherText;
  }

  public List<DbInstitutionEmailDomain> getEmailDomains() {
    return emailDomains;
  }

  public void setEmailDomains(List<DbInstitutionEmailDomain> emailDomains) {
    this.emailDomains = emailDomains;
  }

  public List<DbInstitutionEmailAddress> getEmailAddresses() {
    return emailAddresses;
  }

  public void setEmailAddresses(List<DbInstitutionEmailAddress> emailAddresses) {
    this.emailAddresses = emailAddresses;
  }
}
