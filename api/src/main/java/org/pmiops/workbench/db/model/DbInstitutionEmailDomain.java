package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "institution_email_domain")
public class DbInstitutionEmailDomain {

  private long institutionEmailDomainId;
  private DbInstitution institution;
  private String emailDomain;

  public DbInstitutionEmailDomain() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_email_domain_id", nullable = false)
  public long getInstitutionEmailDomainId() {
    return institutionEmailDomainId;
  }

  public DbInstitutionEmailDomain setInstitutionEmailDomainId(long institutionEmailDomainId) {
    this.institutionEmailDomainId = institutionEmailDomainId;
    return this;
  }

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbInstitutionEmailDomain setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "email_domain", nullable = false)
  public String getEmailDomain() {
    return emailDomain;
  }

  public DbInstitutionEmailDomain setEmailDomain(String emailDomain) {
    this.emailDomain = emailDomain;
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

    DbInstitutionEmailDomain that = (DbInstitutionEmailDomain) o;

    // calling institution.equals() would introduce a cycle here
    return Objects.equals(emailDomain, that.emailDomain)
        && DbInstitution.equalsWithoutEmails(institution, that.institution);
  }

  @Override
  public int hashCode() {
    // calling institution.hashCode() would introduce a cycle here
    return Objects.hash(emailDomain, DbInstitution.hashCodeWithoutEmails(institution));
  }

  @Override
  public String toString() {
    return "DbInstitutionEmailDomain{"
        + "institutionEmailDomainId="
        + institutionEmailDomainId
        + ", institution="
        + institution
        + ", emailDomain='"
        + emailDomain
        + '\''
        + '}';
  }
}
