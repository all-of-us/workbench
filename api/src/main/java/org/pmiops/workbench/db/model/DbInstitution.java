package org.pmiops.workbench.db.model;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.OrganizationType;

@Entity
@Table(name = "institution")
public class DbInstitution {

  private long institutionId; // primary opaque key for DB use only
  private String shortName; // unique key exposed to API
  private String displayName;
  private Short organizationTypeEnum;
  private String organizationTypeOtherText;
  private Short duaTypeEnum;
  private Set<DbInstitutionEmailDomain> emailDomains = Sets.newHashSet();
  private Set<DbInstitutionEmailAddress> emailAddresses = Sets.newHashSet();

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

  @Column(name = "dua_type_enum")
  public DuaType getDuaTypeEnum() {
    return DbStorageEnums.institutionDUATypeFromStorage(duaTypeEnum);
  }

  public DbInstitution setDuaTypeEnum(DuaType institutionDuaType) {
    this.duaTypeEnum = DbStorageEnums.institutionDUATypeToStorage(institutionDuaType);
    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL)
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
   * @param emailDomains the new collection of domains for this Institution
   */
  public DbInstitution setEmailDomains(final Collection<DbInstitutionEmailDomain> emailDomains) {
    final Set<DbInstitutionEmailDomain> attachedDomains =
        Optional.ofNullable(emailDomains)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .map(domain -> domain.setInstitution(this))
            .collect(Collectors.toSet());

    // not sure how this happens, but... Spring
    if (this.emailDomains == null) {
      this.emailDomains = Sets.newHashSet();
    } else {
      // modifies this set so that its value is the intersection of the two sets
      this.emailDomains.retainAll(attachedDomains);
    }

    this.emailDomains.addAll(Sets.difference(attachedDomains, this.emailDomains));

    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL)
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
   * @param emailAddresses the new collection of addresses for this Institution
   */
  public DbInstitution setEmailAddresses(
      final Collection<DbInstitutionEmailAddress> emailAddresses) {

    System.out.println("setEmailAddresses");
    System.out.println("emailAddresses.size()");
    System.out.println(emailAddresses.size());
    System.out.println("emailAddresses");
    System.out.println(emailAddresses);
    System.out.println("I have successfully output emailAddresses");

    final Set<DbInstitutionEmailAddress> attachedAddresses =
        Optional.ofNullable(emailAddresses)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .map(address -> address.setInstitution(this))
            .collect(Collectors.toSet());

    // not sure how this happens, but... Spring
    if (this.emailAddresses == null) {
      this.emailAddresses = Sets.newHashSet();
    } else {
      // modifies this set so that its value is the intersection of the two sets
      this.emailAddresses.retainAll(attachedAddresses);
    }

    this.emailAddresses.addAll(Sets.difference(attachedAddresses, this.emailAddresses));

    return this;
  }

  @Override
  public String toString() {
    return "DbInstitution{"
        + "institutionId="
        + institutionId
        + ", shortName='"
        + shortName
        + '\''
        + ", displayName='"
        + displayName
        + '\''
        + ", organizationTypeEnum="
        + organizationTypeEnum
        + ", organizationTypeOtherText='"
        + organizationTypeOtherText
        + '\''
        + ", duaTypeEnum="
        + duaTypeEnum
        + ", emailDomains="
        + emailDomains
        + ", emailAddresses="
        + emailAddresses
        + '}';
  }
}
