package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "institution_email_domain")
public class DbInstitutionEmailDomain {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_email_domain_id")
  private long institutionEmailDomainId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "institution_id")
  private DbInstitution institution;

  @Column(name = "email_domain", nullable = false)
  private String emailDomain;

  public DbInstitutionEmailDomain() {}

  public DbInstitutionEmailDomain(DbInstitution institution, String emailDomain) {
    this.institution = institution;
    this.emailDomain = emailDomain;
  }

  public DbInstitution getInstitution() {
    return institution;
  }

  public String getEmailDomain() {
    return emailDomain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbInstitutionEmailDomain)) {
      return false;
    }

    DbInstitutionEmailDomain that = (DbInstitutionEmailDomain) o;

    if (institutionEmailDomainId != that.institutionEmailDomainId) {
      return false;
    }
    if (!institution.equals(that.institution)) {
      return false;
    }
    return emailDomain.equals(that.emailDomain);
  }

  @Override
  public int hashCode() {
    int result = (int) (institutionEmailDomainId ^ (institutionEmailDomainId >>> 32));
    result = 31 * result + institution.hashCode();
    result = 31 * result + emailDomain.hashCode();
    return result;
  }
}
