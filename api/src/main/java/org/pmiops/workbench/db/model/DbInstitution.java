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

  @Column(name = "display_name", nullable = false)
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

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @NotNull
  public Set<DbInstitutionEmailDomain> getEmailDomains() {
    return emailDomains;
  }

  /**
   * Effectively: do an in-place this.emailDomains = emailDomains
   *
   * <p>Hibernate doesn't like it when you reassign collections. Instead, modify in-place. First,
   * call retainAll() to subset DB rows to those we wish to keep: the intersection of old and new.
   * Then, call addAll() to add the diff(new - old) rows.
   *
   * <p>https://stackoverflow.com/questions/5587482/hibernate-a-collection-with-cascade-all-delete-orphan-was-no-longer-referenc
   *
   * @param emailDomains the new set of domains for this Institution
   */
  public DbInstitution setEmailDomains(final Set<DbInstitutionEmailDomain> emailDomains) {
    final Set<DbInstitutionEmailDomain> attachedDomains =
        emailDomains.stream()
            .map(domain -> domain.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailDomains.retainAll(attachedDomains);
    this.emailDomains.addAll(Sets.difference(attachedDomains, this.emailDomains));
    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @NotNull
  public Set<DbInstitutionEmailAddress> getEmailAddresses() {
    return emailAddresses;
  }

  /**
   * Effectively: do an in-place this.emailAddresses = emailAddresses
   *
   * <p>Hibernate doesn't like it when you reassign collections. Instead, modify in-place. First,
   * call retainAll() to subset DB rows to those we wish to keep: the intersection of old and new.
   * Then, call addAll() to add the diff(new - old) rows.
   *
   * <p>https://stackoverflow.com/questions/5587482/hibernate-a-collection-with-cascade-all-delete-orphan-was-no-longer-referenc
   *
   * @param emailAddresses the new set of addresses for this Institution
   */
  public DbInstitution setEmailAddresses(final Set<DbInstitutionEmailAddress> emailAddresses) {
    final Set<DbInstitutionEmailAddress> attachedAddresses =
        emailAddresses.stream()
            .map(address -> address.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailAddresses.retainAll(attachedAddresses);
    this.emailAddresses.addAll(Sets.difference(attachedAddresses, this.emailAddresses));
    return this;
  }
}
