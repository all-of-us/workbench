package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_terms_of_service")
public class DbUserTermsOfService {
  private long userTermsOfServiceId;
  private DbUser user;
  private int tosVersion;
  private Timestamp agreementTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_terms_of_service_id")
  public long getUserTermsOfServiceId() {
    return userTermsOfServiceId;
  }

  public void setUserTermsOfServiceId(long userTermsOfServiceId) {
    this.userTermsOfServiceId = userTermsOfServiceId;
  }

  @ManyToOne
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser user) {
    this.user = user;
    // Because this is a bidirectional parent-child relation, when we set the parent reference here,
    // we also want to track this instance as a child in the parent object.
    user.addTermsOfServiceRow(this);
  }

  @Column(name = "tos_version")
  public int getTosVersion() {
    return tosVersion;
  }

  public void setTosVersion(int tosVersion) {
    this.tosVersion = tosVersion;
  }

  @Column(name = "agreement_time")
  public Timestamp getAgreementTime() {
    return agreementTime;
  }

  public void setAgreementTime(Timestamp agreementTime) {
    this.agreementTime = agreementTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbUserTermsOfService that = (DbUserTermsOfService) o;
    return userTermsOfServiceId == that.userTermsOfServiceId
        && user.equals(that.user)
        && tosVersion == that.tosVersion
        && Objects.equals(agreementTime, that.agreementTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userTermsOfServiceId, user, tosVersion, agreementTime);
  }
}
