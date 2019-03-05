package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
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
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
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
  // A nonce which can be used during the account creation flow to verify
  // unauthenticated API calls after account creation, but before initial login.
  private Long creationNonce;
  // The Google email address that the user signs in with.
  private String email;
  // The email address that can be used to contact the user.
  private String contactEmail;
  private Short dataAccessLevel;
  private String givenName;
  private String familyName;
  private String phoneNumber;
  private String currentPosition;
  private String organization;
  private String freeTierBillingProjectName;
  private Short freeTierBillingProjectStatus;
  private Timestamp firstSignInTime;
  private Set<Short> authorities = new HashSet<>();
  private Set<WorkspaceUserRole> workspaceUserRoles = new HashSet<WorkspaceUserRole>();
  private Boolean idVerificationIsValid;
  private Timestamp termsOfServiceCompletionTime;
  private Timestamp trainingCompletionTime;
  private Timestamp demographicSurveyCompletionTime;
  private boolean disabled;
  private Short emailVerificationStatus;
  private Boolean requestedIdVerification;
  private Timestamp idVerificationRequestTime;
  private Set<PageVisit> pageVisits = new HashSet<PageVisit>();

  private List<InstitutionalAffiliation> institutionalAffiliations =
      new ArrayList<InstitutionalAffiliation>();
  private String aboutYou;
  private String areaOfResearch;
  private Boolean twoFactorEnabled = false;
  private Integer clusterCreateRetries;
  private Integer billingProjectRetries;
  private Integer moodleId;
  private Timestamp trainingExpirationTime;
  private String eraCommonsLinkedNihUsername;
  private Timestamp eraCommonsLinkExpireTime;
  private Timestamp eraCommonsCompletionTime;

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

  @Column(name = "creation_nonce")
  public Long getCreationNonce() { return creationNonce; }

  public void setCreationNonce(Long creationNonce) { this.creationNonce = creationNonce; }

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
  public Short getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(Short dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Transient
  public DataAccessLevel getDataAccessLevelEnum() {
    return CommonStorageEnums.dataAccessLevelFromStorage(getDataAccessLevel());
  }

  public void setDataAccessLevelEnum(DataAccessLevel dataAccessLevel) {
    setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel));
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

  @Column(name = "current_position")
  public String getCurrentPosition() {
    return currentPosition;
  }

  public void setCurrentPosition(String currentPosition) {
    this.currentPosition = currentPosition;
  }

  @Column(name = "organization")
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Column(name = "free_tier_billing_project_name")
  public String getFreeTierBillingProjectName() {
    return freeTierBillingProjectName;
  }

  public void setFreeTierBillingProjectName(String freeTierBillingProjectName) {
    this.freeTierBillingProjectName = freeTierBillingProjectName;
  }

  @Column(name = "free_tier_billing_project_status")
  public Short getFreeTierBillingProjectStatus() {
    return freeTierBillingProjectStatus;
  }

  public void setFreeTierBillingProjectStatus(Short freeTierBillingProjectStatus) {
    this.freeTierBillingProjectStatus = freeTierBillingProjectStatus;
  }

  @Transient
  public BillingProjectStatus getFreeTierBillingProjectStatusEnum() {
    return StorageEnums.billingProjectStatusFromStorage(getFreeTierBillingProjectStatus());
  }

  public void setFreeTierBillingProjectStatusEnum(
      BillingProjectStatus freeTierBillingProjectStatus) {
    setFreeTierBillingProjectStatus(
        StorageEnums.billingProjectStatusToStorage(freeTierBillingProjectStatus));
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
  public Set<Short> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Short> newAuthorities) {
    this.authorities = newAuthorities;
  }

  @Transient
  public Set<Authority> getAuthoritiesEnum() {
    Set<Short> from = getAuthorities();
    if (from == null) {
      return null;
    }
    return from
        .stream()
        .map(StorageEnums::authorityFromStorage)
        .collect(Collectors.toSet());
  }

  public void setAuthoritiesEnum(Set<Authority> newAuthorities) {
    this.setAuthorities(
        newAuthorities
        .stream()
        .map(StorageEnums::authorityToStorage)
        .collect(Collectors.toSet()));
  }

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "user")
  @Column(name="page_id")
  public Set<PageVisit> getPageVisits() {
    return pageVisits;
  }

  public void setPageVisits(Set<PageVisit> newPageVisits) {
    this.pageVisits = newPageVisits;
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

  @Column(name = "id_verification_is_valid")
  public Boolean getIdVerificationIsValid() {
    return idVerificationIsValid;
  }
  public void setIdVerificationIsValid(Boolean value) {
    idVerificationIsValid = value;
  }

  @Column(name = "terms_of_service_completion_time")
  public Timestamp getTermsOfServiceCompletionTime() {
    return termsOfServiceCompletionTime;
  }

  public void setTermsOfServiceCompletionTime(Timestamp termsOfServiceCompletionTime) {
    this.termsOfServiceCompletionTime = termsOfServiceCompletionTime;
  }

  @Column(name = "training_completion_time")
  public Timestamp getTrainingCompletionTime() {
    return trainingCompletionTime;
  }

  public void setTrainingCompletionTime(Timestamp trainingCompletionTime) {
    this.trainingCompletionTime = trainingCompletionTime;
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

  @Column(name = "email_verification_status")
  public Short getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(Short emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }

  @Transient
  public EmailVerificationStatus getEmailVerificationStatusEnum() {
    return StorageEnums.emailVerificationStatusFromStorage(getEmailVerificationStatus());
  }

  public void setEmailVerificationStatusEnum(EmailVerificationStatus emailVerificationStatus) {
    setEmailVerificationStatus(
        StorageEnums.emailVerificationStatusToStorage(emailVerificationStatus));
  }

  @OneToMany(fetch = FetchType.EAGER, mappedBy = "userId", orphanRemoval = true, cascade = CascadeType.ALL)
  @OrderColumn(name="order_index")
  public List<InstitutionalAffiliation> getInstitutionalAffiliations() {
    return institutionalAffiliations;
  }

  public void setInstitutionalAffiliations(List<InstitutionalAffiliation> newInstitutionalAffiliations) {
    this.institutionalAffiliations = newInstitutionalAffiliations;
  }

  public void clearInstitutionalAffiliations() {
    this.institutionalAffiliations.clear();
  }

  public void addInstitutionalAffiliation(InstitutionalAffiliation newInstitutionalAffiliation) {
    this.institutionalAffiliations.add(newInstitutionalAffiliation);
  }

  @Column(name = "about_you")
  public String getAboutYou() {
    return aboutYou;
  }

  public void setAboutYou(String aboutYou) {
    this.aboutYou = aboutYou;
  }

  @Column(name = "area_of_research")
  public String getAreaOfResearch() {
    return areaOfResearch;
  }

  public void setAreaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
  }

  @Column(name = "requested_id_verification")
  public Boolean getRequestedIdVerification() {
    return requestedIdVerification;
  }

  public void setRequestedIdVerification(Boolean requestedIdVerification) {
    this.requestedIdVerification = requestedIdVerification;
  }

  @Column(name = "two_factor_enabled")
  public Boolean getTwoFactorEnabled() {
    return twoFactorEnabled;
  }

  public void setTwoFactorEnabled(Boolean twoFactorEnabled) {
    this.twoFactorEnabled = twoFactorEnabled;
  }

  @Column(name = "cluster_create_retries")
  public Integer getClusterCreateRetries() {
    return clusterCreateRetries;
  }

  public void setClusterCreateRetries(Integer clusterCreateRetries) {
    this.clusterCreateRetries = clusterCreateRetries;
  }

  @Column(name = "billing_project_retries")
  public Integer getBillingProjectRetries() {
    return billingProjectRetries;
  }

  public void setBillingProjectRetries(Integer billingProjectRetries) {
    this.billingProjectRetries = billingProjectRetries;
  }

  @Column(name = "id_verification_request_time")
  public Timestamp getIdVerificationRequestTime() {
    return idVerificationRequestTime;
  }

  public void setIdVerificationRequestTime(Timestamp idVerificationRequestTime) {
    this.idVerificationRequestTime = idVerificationRequestTime;
  }

  @Column(name = "moodle_id")
  public Integer getMoodleId() { return moodleId; }

  public void setMoodleId(Integer moodleId) {
    this.moodleId = moodleId;
  }

  @Column(name = "training_expiration_time")
  public Timestamp getTrainingExpirationTime() { return trainingExpirationTime; }

  public void setTrainingExpirationTime( Timestamp trainingExpirationTime) {
    this.trainingExpirationTime = trainingExpirationTime;
  }

  @Column(name = "era_commons_linked_nih_username")
  public String getEraCommonsLinkedNihUsername() { return eraCommonsLinkedNihUsername; }

  public void setEraCommonsLinkedNihUsername( String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
  }

  @Column(name = "era_commons_link_expire_time")
  public Timestamp getEraCommonsLinkExpireTime() { return eraCommonsLinkExpireTime; }

  public void setEraCommonsLinkExpireTime( Timestamp eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
  }

  @Column(name = "era_commons_completion_time")
  public Timestamp getEraCommonsCompletionTime() { return eraCommonsCompletionTime; }

  public void setEraCommonsCompletionTime(Timestamp eraCommonsCompletionTime) {
    this.eraCommonsCompletionTime = eraCommonsCompletionTime;
  }
}
