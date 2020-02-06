package org.pmiops.workbench.db.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "institution")
public class DbInstitution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_id")
  private long dbId; // primary opaque key for DB use only

  @Column(name = "short_name", nullable = false, unique = true)
  private String shortName; // unique key exposed to API

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "organization_type_enum")
  private Short organizationTypeEnum;

  @Column(name = "organization_type_other_text")
  private String organizationTypeOtherText;

  @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
  private Set<DbInstitutionEmailDomain> emailDomains;

  @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
  private Set<DbInstitutionEmailAddress> emailAddresses;

  public DbInstitution() {}

  public DbInstitution(final String shortName, final String displayName) {
    setShortName(shortName);
    setDisplayName(displayName);
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  public Set<DbInstitutionEmailDomain> getEmailDomains() {
    return emailDomains;
  }

  public void setEmailDomains(Iterable<DbInstitutionEmailDomain> emailDomains) {
    this.emailDomains =
        StreamSupport.stream(emailDomains.spliterator(), false).collect(Collectors.toSet());
  }

  public Set<DbInstitutionEmailAddress> getEmailAddresses() {
    return emailAddresses;
  }

  public void setEmailAddresses(Iterable<DbInstitutionEmailAddress> emailAddresses) {
    this.emailAddresses =
        StreamSupport.stream(emailAddresses.spliterator(), false).collect(Collectors.toSet());
  }
}
