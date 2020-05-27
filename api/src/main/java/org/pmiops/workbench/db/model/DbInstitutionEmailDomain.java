package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "institution_email_domain")
public class DbInstitutionEmailDomain {

  private long institutionEmailDomainId;
  private long institutionId;
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

  @Column(name = "institution_id", nullable = false)
  public long getInstitutionId() {
    return institutionId;
  }

  public DbInstitutionEmailDomain setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
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

  // logical equality: data members without the ID field

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DbInstitutionEmailDomain that = (DbInstitutionEmailDomain) o;

    return Objects.equals(emailDomain, that.emailDomain)
        && Objects.equals(institutionId, that.institutionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailDomain, institutionId);
  }
}
