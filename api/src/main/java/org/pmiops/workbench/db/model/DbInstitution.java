package org.pmiops.workbench.db.model;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.pmiops.workbench.model.OrganizationType;

@Entity
@Table(name = "institution")
public class DbInstitution {

  private long institutionId; // primary opaque key for DB use only
  private String shortName; // unique key exposed to API
  private String displayName;
  private Short organizationTypeEnum;
  private String organizationTypeOtherText;
  @NotNull private Set<DbInstitutionEmailDomain> emailDomains = Sets.newHashSet();
  @NotNull private Set<DbInstitutionEmailAddress> emailAddresses = Sets.newHashSet();

  public DbInstitution() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_id")
  public long getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
  }

  @Column(name = "short_name", nullable = false, unique = true)
  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution shortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  @Column(name = "display_name", nullable = false)
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  @Column(name = "organization_type_enum")
  public OrganizationType getOrganizationTypeEnum() {
    return DbStorageEnums.organizationTypeFromStorage(organizationTypeEnum);
  }

  public void setOrganizationTypeEnum(OrganizationType type) {
    this.organizationTypeEnum = DbStorageEnums.organizationTypeToStorage(type);
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution organizationTypeEnum(OrganizationType type) {
    this.organizationTypeEnum = DbStorageEnums.organizationTypeToStorage(type);
    return this;
  }

  @Column(name = "organization_type_other_text")
  public String getOrganizationTypeOtherText() {
    return organizationTypeOtherText;
  }

  public void setOrganizationTypeOtherText(String organizationTypeOtherText) {
    this.organizationTypeOtherText = organizationTypeOtherText;
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution organizationTypeOtherText(String organizationTypeOtherText) {
    this.organizationTypeOtherText = organizationTypeOtherText;
    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  public Set<DbInstitutionEmailDomain> getEmailDomains() {
    return emailDomains;
  }

  /**
   * Effectively: do an in-place this.emailDomains = emailDomains
   *
   * Hibernate doesn't like it when you reassign collections. Instead, modify in-place. First, call
   * retainAll() to subset DB rows to those we wish to keep: the intersection of old and new. Then,
   * call addAll() to add the diff(new - old) rows.
   *
   * <p>https://stackoverflow.com/questions/5587482/hibernate-a-collection-with-cascade-all-delete-orphan-was-no-longer-referenc
   *
   * @param emailDomains the new set of domains for this Institution
   */
  public void setEmailDomains(final Set<DbInstitutionEmailDomain> emailDomains) {
    final Set<DbInstitutionEmailDomain> attachedDomains =
        emailDomains.stream()
            .map(domain -> domain.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailDomains.retainAll(attachedDomains);
    this.emailDomains.addAll(Sets.difference(attachedDomains, this.emailDomains));
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution emailDomains(Set<DbInstitutionEmailDomain> emailDomains) {
    setEmailDomains(emailDomains);
    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  public Set<DbInstitutionEmailAddress> getEmailAddresses() {
    return emailAddresses;
  }

  /**
   * Effectively: do an in-place this.emailAddresses = emailAddresses
   *
   * Hibernate doesn't like it when you reassign collections. Instead, modify in-place. First, call
   * retainAll() to subset DB rows to those we wish to keep: the intersection of old and new. Then,
   * call addAll() to add the diff(new - old) rows.
   *
   * <p>https://stackoverflow.com/questions/5587482/hibernate-a-collection-with-cascade-all-delete-orphan-was-no-longer-referenc
   *
   * @param emailAddresses the new set of addresses for this Institution
   */
  public void setEmailAddresses(final Set<DbInstitutionEmailAddress> emailAddresses) {
    final Set<DbInstitutionEmailAddress> attachedAddresses =
        emailAddresses.stream()
            .map(address -> address.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailAddresses.retainAll(attachedAddresses);
    this.emailAddresses.addAll(Sets.difference(attachedAddresses, this.emailAddresses));
  }

  /** Builder method to help streamline the building of a DbInstitution. */
  public DbInstitution emailAddresses(Set<DbInstitutionEmailAddress> emailAddresses) {
    setEmailAddresses(emailAddresses);
    return this;
  }
}
