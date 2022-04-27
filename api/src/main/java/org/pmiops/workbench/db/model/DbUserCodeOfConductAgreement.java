package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_code_of_conduct_agreement")
public class DbUserCodeOfConductAgreement {
  private long userDuccAgreementId;
  private DbUser user;
  private String userGivenName;
  private String userFamilyName;
  private String userInitials;
  private boolean userNameOutOfDate;
  private int signedVersion;
  private Timestamp completionTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getUserDuccAgreementId() {
    return userDuccAgreementId;
  }

  public DbUserCodeOfConductAgreement setUserDuccAgreementId(long userDuccAgreementId) {
    this.userDuccAgreementId = userDuccAgreementId;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbUserCodeOfConductAgreement setUser(DbUser dbUser) {
    this.user = dbUser;
    return this;
  }

  @Column(name = "user_given_name")
  public String getUserGivenName() {
    return userGivenName;
  }

  public DbUserCodeOfConductAgreement setUserGivenName(String userGivenName) {
    this.userGivenName = userGivenName;
    return this;
  }

  @Column(name = "user_family_name")
  public String getUserFamilyName() {
    return userFamilyName;
  }

  public DbUserCodeOfConductAgreement setUserFamilyName(String userFamilyName) {
    this.userFamilyName = userFamilyName;
    return this;
  }

  @Column(name = "user_initials")
  public String getUserInitials() {
    return userInitials;
  }

  public DbUserCodeOfConductAgreement setUserInitials(String userInitials) {
    this.userInitials = userInitials;
    return this;
  }

  // This is set to 'true' whenever user_given_name or user_family_name are not the same as the
  // given_name or family_name on the User entry referenced by user_id.
  @Column(name = "user_name_out_of_date")
  public boolean isUserNameOutOfDate() {
    return userNameOutOfDate;
  }

  public DbUserCodeOfConductAgreement setUserNameOutOfDate(boolean userNameOutOfDate) {
    this.userNameOutOfDate = userNameOutOfDate;
    return this;
  }

  @Column(name = "signed_version")
  public int getSignedVersion() {
    return signedVersion;
  }

  public DbUserCodeOfConductAgreement setSignedVersion(int signedVersion) {
    this.signedVersion = signedVersion;
    return this;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public DbUserCodeOfConductAgreement setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbUserCodeOfConductAgreement that = (DbUserCodeOfConductAgreement) o;
    return userDuccAgreementId == that.userDuccAgreementId
        && user.equals(that.user)
        && userNameOutOfDate == that.userNameOutOfDate
        && signedVersion == that.signedVersion
        && userGivenName.equals(that.userGivenName)
        && userFamilyName.equals(that.userFamilyName)
        && userInitials.equals(that.userInitials)
        && completionTime.equals(that.completionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        userDuccAgreementId,
        user,
        userGivenName,
        userFamilyName,
        userInitials,
        userNameOutOfDate,
        signedVersion,
        completionTime);
  }
}
