package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;

@Entity
@Table(name = "user")
public class User {

  private long userId;
  private int version;
  // The Google email address that the user signs in with.
  private String email;
  // The email address that can be used to contact the user.
  private String contactEmail;
  private DataAccessLevel dataAccessLevel;
  private String givenName;
  private String familyName;
  private String phoneNumber;
  private String freeTierBillingProjectName;
  private BillingProjectStatus freeTierBillingProjectStatus;
  private Timestamp firstSignInTime;
  private Set<Authority> authorities = new HashSet<Authority>();
  private Set<WorkspaceUserRole> workspaceUserRoles = new HashSet<WorkspaceUserRole>();
  private String blockscoreId;
  private Boolean blockscoreVerificationIsValid;
  private Timestamp termsOfServiceCompletionTime;
  private Timestamp ethicsTrainingCompletionTime;
  private Timestamp demographicSurveyCompletionTime;
  private boolean disabled;
  private Timestamp disabledTime;
  private Long disablingAdminId;
  private EmailVerificationStatus emailVerificationStatus;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Version
  @Column(name = "version")
  public int getVersion() { return version; }

  public void setVersion(int version) { this.version = version; }

  @Column(name = "email")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Column(name = "contact_email")
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Column(name = "data_access_level")
  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Column(name = "given_name")
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Column(name = "family_name")
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  // TODO: consider dropping this (do we want researcher phone numbers?)
  @Column(name = "phone_number")
  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @Column(name = "free_tier_billing_project_name")
  public String getFreeTierBillingProjectName() {
    return freeTierBillingProjectName;
  }

  public void setFreeTierBillingProjectName(String freeTierBillingProjectName) {
    this.freeTierBillingProjectName = freeTierBillingProjectName;
  }

  @Column(name = "free_tier_billing_project_status")
  public BillingProjectStatus getFreeTierBillingProjectStatus() {
    return freeTierBillingProjectStatus;
  }

  public void setFreeTierBillingProjectStatus(BillingProjectStatus freeTierBillingProjectStatus) {
    this.freeTierBillingProjectStatus = freeTierBillingProjectStatus;
  }

  @Column(name = "first_sign_in_time")
  public Timestamp getFirstSignInTime() {
    return firstSignInTime;
  }

  public void setFirstSignInTime(Timestamp firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
  }

  // Authorities (special permissions) are granted using api/project.rb set-authority.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "authority")
  public Set<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Authority> newAuthorities) {
    this.authorities = newAuthorities;
  }

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
  public Set<WorkspaceUserRole> getWorkspaceUserRoles() {
    return workspaceUserRoles;
  }

  /**
   * Necessary for Spring initialization of the object.
   * Not actually supported because it won't delete old entries.
   */
  public void setWorkspaceUserRoles(Set<WorkspaceUserRole> userRoles) {
    this.workspaceUserRoles = userRoles;
  }

  @Column(name = "blockscore_id")
  public String getBlockscoreId() {
    return blockscoreId;
  }
  public void setBlockscoreId(String blockscoreId) {
    this.blockscoreId = blockscoreId;
  }

  @Column(name = "blockscore_verification_is_valid")
  public Boolean getBlockscoreVerificationIsValid() {
    return blockscoreVerificationIsValid;
  }
  public void setBlockscoreVerificationIsValid(Boolean value) {
    blockscoreVerificationIsValid = value;
  }

  @Column(name = "terms_of_service_completion_time")
  public Timestamp getTermsOfServiceCompletionTime() {
    return termsOfServiceCompletionTime;
  }

  public void setTermsOfServiceCompletionTime(Timestamp termsOfServiceCompletionTime) {
    this.termsOfServiceCompletionTime = termsOfServiceCompletionTime;
  }

  @Column(name = "ethics_training_completion_time")
  public Timestamp getEthicsTrainingCompletionTime() {
    return ethicsTrainingCompletionTime;
  }

  public void setEthicsTrainingCompletionTime(Timestamp ethicsTrainingCompletionTime) {
    this.ethicsTrainingCompletionTime = ethicsTrainingCompletionTime;
  }

  @Column(name = "demographic_survey_completion_time")
  public Timestamp getDemographicSurveyCompletionTime() {
    return demographicSurveyCompletionTime;
  }

  public void setDemographicSurveyCompletionTime(Timestamp demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
  }

  @Column(name = "disabled")
  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @Column(name = "disabled_time")
  public Timestamp getDisabledTime() {
    return disabledTime;
  }

  public void setDisabledTime(Timestamp disabledTime) {
    this.disabledTime = disabledTime;
  }

  @Column(name = "disabling_admin_id")
  public Long getDisablingAdminId() {
    return disablingAdminId;
  }

  public void setDisablingAdminId(Long disablingAdminId) {
    this.disablingAdminId = disablingAdminId;
  }

  @Column(name = "email_verification_status")
  public EmailVerificationStatus getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(EmailVerificationStatus emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }
}
