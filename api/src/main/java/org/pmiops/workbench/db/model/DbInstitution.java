package org.pmiops.workbench.db.model;

import com.google.common.base.Objects;
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
import org.jetbrains.annotations.NotNull;
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

  @Column(name = "dua_type_enum")
  public DuaType getDuaTypeEnum() {
    return DbStorageEnums.institutionDUATypeFromStorage(duaTypeEnum);
  }

  public DbInstitution setDuaTypeEnum(DuaType institutionDuaType) {
    this.duaTypeEnum = DbStorageEnums.institutionDUATypeToStorage(institutionDuaType);
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

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL)
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
   * @param emailDomains the new collection of domains for this Institution
   */
  public DbInstitution setEmailDomains(final Collection<DbInstitutionEmailDomain> emailDomains) {
    final Set<DbInstitutionEmailDomain> attachedDomains =
        Optional.ofNullable(emailDomains)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .map(domain -> domain.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailDomains.retainAll(attachedDomains);
    this.emailDomains.addAll(Sets.difference(attachedDomains, this.emailDomains));

    return this;
  }

  @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL)
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
   * @param emailAddresses the new collection of addresses for this Institution
   */
  public DbInstitution setEmailAddresses(
      final Collection<DbInstitutionEmailAddress> emailAddresses) {
    final Set<DbInstitutionEmailAddress> attachedAddresses =
        Optional.ofNullable(emailAddresses)
            .map(Collection::stream)
            .orElse(Stream.empty())
            .map(address -> address.setInstitution(this))
            .collect(Collectors.toSet());
    // modifies this set so that its value is the intersection of the two sets
    this.emailAddresses.retainAll(attachedAddresses);
    this.emailAddresses.addAll(Sets.difference(attachedAddresses, this.emailAddresses));

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

    final boolean domainsEq = Objects.equal(emailDomains, that.emailDomains);
    System.out.println("DBInst emailDomains eq=" + domainsEq);
    if (!domainsEq) {
      for (DbInstitutionEmailDomain domain : emailDomains) {
        System.out.println("domain = " + domain);
      }
      for (DbInstitutionEmailDomain domain : that.emailDomains) {
        System.out.println("that domain = " + domain);
      }
    }

    final boolean addrsEq = Objects.equal(emailAddresses, that.emailAddresses);
    System.out.println("DBInst emailAddresses eq=" + addrsEq);
    if (!addrsEq) {
      for (DbInstitutionEmailAddress addr : emailAddresses) {
        System.out.println("addr = " + addr);
      }

      for (DbInstitutionEmailAddress addr : that.emailAddresses) {
        System.out.println("that addr = " + addr);
      }
    }

    return Objects.equal(shortName, that.shortName)
        && Objects.equal(displayName, that.displayName)
        && Objects.equal(organizationTypeEnum, that.organizationTypeEnum)
        && Objects.equal(organizationTypeOtherText, that.organizationTypeOtherText)
        && Objects.equal(duaTypeEnum, that.duaTypeEnum)
        && Objects.equal(emailDomains, that.emailDomains)
        && Objects.equal(emailAddresses, that.emailAddresses);
  }

  // to prevent cycles in the emails' equals() methods
  public boolean equalsWithoutEmails(Object o) {
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
        && Objects.equal(duaTypeEnum, that.duaTypeEnum);
  }

  public static boolean equalsWithoutEmails(final DbInstitution a, final DbInstitution b) {
    if (a == null) {
      return (b == null);
    } else {
      return a.equalsWithoutEmails(b);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        shortName,
        displayName,
        organizationTypeEnum,
        organizationTypeOtherText,
        duaTypeEnum,
        emailDomains,
        emailAddresses);
  }

  // to prevent cycles in the emails' hashCode() methods
  public int hashCodeWithoutEmails() {
    return Objects.hashCode(
        shortName, displayName, organizationTypeEnum, organizationTypeOtherText, duaTypeEnum);
  }

  public static Object hashCodeWithoutEmails(final DbInstitution institution) {
    if (institution == null) {
      return 0;
    } else {
      return institution.hashCodeWithoutEmails();
    }
  }
}
