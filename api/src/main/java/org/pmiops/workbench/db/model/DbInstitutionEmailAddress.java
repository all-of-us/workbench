package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "institution_email_address")
public class DbInstitutionEmailAddress {

  private long institutionEmailAddressId;
  private DbInstitution institution;
  private DbAccessTier accessTier;
  private String emailAddress;

  public DbInstitutionEmailAddress() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_email_address_id", nullable = false)
  public long getInstitutionEmailAddressId() {
    return institutionEmailAddressId;
  }

  public DbInstitutionEmailAddress setInstitutionEmailAddressId(long institutionEmailAddressId) {
    this.institutionEmailAddressId = institutionEmailAddressId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbInstitutionEmailAddress setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "email_address", nullable = false)
  public String getEmailAddress() {
    return emailAddress;
  }

  public DbInstitutionEmailAddress setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "access_tier_id", nullable = false)
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbInstitutionEmailAddress setAccessTier(DbAccessTier accessTier) {
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

    DbInstitutionEmailAddress that = (DbInstitutionEmailAddress) o;

    return Objects.equals(emailAddress, that.emailAddress)
        && Objects.equals(institution, that.institution) && Objects.equals(accessTier, that.accessTier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessTier, emailAddress, institution);
  }
}
