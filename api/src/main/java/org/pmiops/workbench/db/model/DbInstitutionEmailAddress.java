package org.pmiops.workbench.db.model;

import javax.persistence.*;

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

  public DbInstitutionEmailAddress(DbInstitution institution, String emailAddress) {
    this.setInstitution(institution);
    this.setEmailAddress(emailAddress);
  }

  public long getInstitutionEmailAddressId() {
    return institutionEmailAddressId;
  }

  public void setInstitutionEmailAddressId(long institutionEmailAddressId) {
    this.institutionEmailAddressId = institutionEmailAddressId;
  }

  public DbInstitution getInstitution() {
    return institution;
  }

  public void setInstitution(DbInstitution institution) {
    this.institution = institution;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }
}
