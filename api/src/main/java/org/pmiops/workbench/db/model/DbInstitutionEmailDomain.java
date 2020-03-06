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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DbInstitutionEmailDomain that = (DbInstitutionEmailDomain) o;

    return Objects.equals(institution, that.institution)
        && Objects.equals(emailDomain, that.emailDomain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institution, emailDomain);
  }
}
