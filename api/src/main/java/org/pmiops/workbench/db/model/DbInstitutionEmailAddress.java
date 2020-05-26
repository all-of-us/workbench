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
@Table(name = "institution_email_address")
public class DbInstitutionEmailAddress {

  private long institutionEmailAddressId;
  private DbInstitution institution;
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

  @ManyToOne(cascade = CascadeType.ALL)
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

    System.out.println("emailAddress=" + emailAddress);
    System.out.println("emailAddress eq=" + Objects.equals(emailAddress, that.emailAddress));
    System.out.println(
        "institutionShortName eq="
            + Objects.equals(institution.getShortName(), that.institution.getShortName()));
    System.out.println(
        "institutionShortName eq2="
            + ((institution != null)
                && (that.institution != null)
                && Objects.equals(institution.getShortName(), that.institution.getShortName())));

    // calling institution.equals() would introduce a cycle here
    return Objects.equals(emailAddress, that.emailAddress)
        && DbInstitution.equalsWithoutEmails(institution, that.institution);
  }

  @Override
  public int hashCode() {
    // calling institution.hashCode() would introduce a cycle here
    return Objects.hash(emailAddress, DbInstitution.hashCodeWithoutEmails(institution));
  }

  @Override
  public String toString() {
    return "DbInstitutionEmailAddress{"
        + "institutionEmailAddressId="
        + institutionEmailAddressId
        + ", institution="
        + institution
        + ", emailAddress='"
        + emailAddress
        + '\''
        + '}';
  }
}
