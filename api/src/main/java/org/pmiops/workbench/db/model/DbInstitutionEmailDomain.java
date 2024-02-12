package org.pmiops.workbench.db.model;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "institution_email_domain")
public class DbInstitutionEmailDomain {

  private long institutionEmailDomainId;
  private DbInstitution institution;
  private DbAccessTier accessTier;
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

  @ManyToOne
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

  @ManyToOne()
  @JoinColumn(name = "access_tier_id", nullable = false)
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbInstitutionEmailDomain setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
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

    return Objects.equals(emailDomain, that.emailDomain)
        && Objects.equals(institution, that.institution)
        && Objects.equals(accessTier, that.accessTier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessTier, emailDomain, institution);
  }
}
