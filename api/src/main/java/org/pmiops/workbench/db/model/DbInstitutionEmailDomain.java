package org.pmiops.workbench.db.model;

import javax.persistence.*;

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

  public DbInstitutionEmailDomain(DbInstitution institution, String emailAddress) {
    this.setInstitution(institution);
    this.setEmailDomain(emailAddress);
  }

  public long getInstitutionEmailDomainId() {
    return institutionEmailDomainId;
  }

  public void setInstitutionEmailDomainId(long institutionEmailDomainId) {
    this.institutionEmailDomainId = institutionEmailDomainId;
  }

  public DbInstitution getInstitution() {
    return institution;
  }

  public void setInstitution(DbInstitution institution) {
    this.institution = institution;
  }

  public String getEmailDomain() {
    return emailDomain;
  }

  public void setEmailDomain(String emailDomain) {
    this.emailDomain = emailDomain;
  }
}
