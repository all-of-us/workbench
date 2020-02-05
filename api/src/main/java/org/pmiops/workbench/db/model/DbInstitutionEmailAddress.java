package org.pmiops.workbench.db.model;

import java.util.Objects;
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
@Table(name = "institution_email_address")
public class DbInstitutionEmailAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_email_address_id")
  private long institutionEmailAddressId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "institution_id")
  private DbInstitution institution;

  @Column(name = "email_address", nullable = false)
  private String emailAddress;

  public DbInstitutionEmailAddress() {}

  public DbInstitutionEmailAddress(DbInstitution institution, String emailAddress) {
    this.institution = institution;
    this.emailAddress = emailAddress;
  }

  public DbInstitution getInstitution() {
    return institution;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbInstitutionEmailAddress)) {
      return false;
    }

    DbInstitutionEmailAddress that = (DbInstitutionEmailAddress) o;

    return institutionEmailAddressId == that.institutionEmailAddressId
        && institution.equals(that.institution)
        && emailAddress.equals(that.emailAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionEmailAddressId, institution, emailAddress);
  }
}
