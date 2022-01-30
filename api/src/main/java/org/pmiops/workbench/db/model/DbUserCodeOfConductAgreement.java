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

  public void setUserDuccAgreementId(long userDuccAgreementId) {
    this.userDuccAgreementId = userDuccAgreementId;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser dbUser) {
    this.user = dbUser;
  }

  @Column(name = "user_given_name")
  public String getUserGivenName() {
    return userGivenName;
  }

  public void setUserGivenName(String userGivenName) {
    this.userGivenName = userGivenName;
  }

  @Column(name = "user_family_name")
  public String getUserFamilyName() {
    return userFamilyName;
  }

  public void setUserFamilyName(String userFamilyName) {
    this.userFamilyName = userFamilyName;
  }

  @Column(name = "user_initials")
  public String getUserInitials() {
    return userInitials;
  }

  public void setUserInitials(String userInitials) {
    this.userInitials = userInitials;
  }

  // This is set to 'true' whenever user_given_name or user_family_name are not the same as the
  // given_name or family_name on the User entry referenced by user_id.
  @Column(name = "user_name_out_of_date")
  public boolean isUserNameOutOfDate() {
    return userNameOutOfDate;
  }

  public void setUserNameOutOfDate(boolean userNameOutOfDate) {
    this.userNameOutOfDate = userNameOutOfDate;
  }

  @Column(name = "signed_version")
  public int getSignedVersion() {
    return signedVersion;
  }

  public void setSignedVersion(int signedVersion) {
    this.signedVersion = signedVersion;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
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
