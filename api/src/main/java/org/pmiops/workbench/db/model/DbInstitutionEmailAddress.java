package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "institution_email_address")
public class DbInstitutionEmailAddress {

  private long institutionEmailAddressId;
  private long institutionId;
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

  @Column(name = "institution_id", nullable = false)
  public long getInstitutionId() {
    return institutionId;
  }

  public DbInstitutionEmailAddress setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
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

  // logical equality: data members without the ID field

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
        && Objects.equals(institutionId, that.institutionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailAddress, institutionId);
  }
}
