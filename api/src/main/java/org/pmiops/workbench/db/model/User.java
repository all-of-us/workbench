package org.pmiops.workbench.db.model;

import com.google.gson.Gson;
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

  /**
   * This is a Gson compatible class for encoding a JSON blob which is stored in MySQL. This
   * represents cluster configuration overrides we support on a per-user basis for their notebook
   * cluster. Corresponds to Leonardo's MachineConfig model. All fields are optional.
   *
   * Any changes to this class should produce backwards-compatible JSON.
   */
  public static class ClusterConfig {
    // Master persistent disk size in GB.
    public Integer masterDiskSize;
    // GCE machine type, e.g. n1-standard-2.
    public String machineType;
  }

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
  private Set<WorkspaceUserRole> workspaceUserRoles = new HashSet<>();
  private Boolean idVerificationIsValid;
  private Timestamp termsOfServiceCompletionTime;
  private Timestamp demographicSurveyCompletionTime;
  private boolean disabled;
  private Short emailVerificationStatus;
  private Set<PageVisit> pageVisits = new HashSet<>();
  private String clusterConfigDefault;

  private List<InstitutionalAffiliation> institutionalAffiliations = new ArrayList<>();
  private String aboutYou;
  private String areaOfResearch;
  private Boolean twoFactorEnabled = false;
  private Integer clusterCreateRetries;
  private Integer billingProjectRetries;
  private Integer moodleId;

  // Access module fields go here. See http://broad.io/aou-access-modules for docs.
  private String eraCommonsLinkedNihUsername;
  private Timestamp eraCommonsLinkExpireTime;
  private Timestamp eraCommonsCompletionTime;
  private Timestamp betaAccessRequestTime;
  private Timestamp betaAccessBypassTime;
  private Timestamp dataUseAgreementCompletionTime;
  private Timestamp dataUseAgreementBypassTime;
  private Timestamp complianceTrainingCompletionTime;
  private Timestamp complianceTrainingBypassTime;
  private Timestamp complianceTrainingExpirationTime;
  private Timestamp eraCommonsBypassTime;
  private Timestamp emailVerificationCompletionTime;
  private Timestamp emailVerificationBypassTime;
  private Timestamp idVerificationCompletionTime;
  private Timestamp idVerificationBypassTime;
  private Timestamp twoFactorAuthCompletionTime;
  private Timestamp twoFactorAuthBypassTime;

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

  @Column(name = "cluster_config_default")
  public String getClusterConfigDefaultRaw() {
    return clusterConfigDefault;
  }
  public void setClusterConfigDefaultRaw(String value) {
    clusterConfigDefault = value;
  }

  @Transient
  public ClusterConfig getClusterConfigDefault() {
    if (clusterConfigDefault == null) {
      return null;
    }
    return new Gson().fromJson(clusterConfigDefault, ClusterConfig.class);
  }
  public void setClusterConfigDefault(ClusterConfig value) {
    String rawValue = null;
    if (value != null) {
      rawValue = new Gson().toJson(value);
    }
    setClusterConfigDefaultRaw(rawValue);
  }

  @Column(name = "terms_of_service_completion_time")
  public Timestamp getTermsOfServiceCompletionTime() {
    return termsOfServiceCompletionTime;
  }

  public void setTermsOfServiceCompletionTime(Timestamp termsOfServiceCompletionTime) {
    this.termsOfServiceCompletionTime = termsOfServiceCompletionTime;
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

  @Column(name = "beta_access_request_time")
  public Timestamp getBetaAccessRequestTime() {
    return betaAccessRequestTime;
  }

  public void setBetaAccessRequestTime(Timestamp betaAccessRequestTime) {
    this.betaAccessRequestTime = betaAccessRequestTime;
  }

  @Column(name = "moodle_id")
  public Integer getMoodleId() { return moodleId; }

  public void setMoodleId(Integer moodleId) {
    this.moodleId = moodleId;
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

  @Column(name = "data_use_agreement_completion_time")
  public Timestamp getDataUseAgreementCompletionTime() {return dataUseAgreementCompletionTime; }

  public void setDataUseAgreementCompletionTime(Timestamp dataUseAgreementCompletionTime) {
    this.dataUseAgreementCompletionTime = dataUseAgreementCompletionTime;
  }

  @Column(name = "data_use_agreement_bypass_time")
  public Timestamp getDataUseAgreementBypassTime() {return dataUseAgreementBypassTime; }

  public void setDataUseAgreementBypassTime(Timestamp dataUseAgreementBypassTime) {
    this.dataUseAgreementBypassTime = dataUseAgreementBypassTime;
  }

  @Column(name = "compliance_training_completion_time")
  public Timestamp getComplianceTrainingCompletionTime() {return complianceTrainingCompletionTime; }

  public void setComplianceTrainingCompletionTime(Timestamp complianceTrainingCompletionTime) {
    this.complianceTrainingCompletionTime = complianceTrainingCompletionTime;
  }

  @Column(name = "compliance_training_bypass_time")
  public Timestamp getComplianceTrainingBypassTime() {return complianceTrainingBypassTime; }

  public void setComplianceTrainingBypassTime(Timestamp complianceTrainingBypassTime) {
    this.complianceTrainingBypassTime = complianceTrainingBypassTime;
  }

  @Column(name = "compliance_training_expiration_time")
  public Timestamp getComplianceTrainingExpirationTime() {return complianceTrainingExpirationTime; }

  public void setComplianceTrainingExpirationTime(Timestamp complianceTrainingExpirationTime) {
    this.complianceTrainingExpirationTime = complianceTrainingExpirationTime;
  }

  @Column(name = "beta_access_bypass_time")
  public Timestamp getBetaAccessBypassTime() {return betaAccessBypassTime; }

  public void setBetaAccessBypassTime(Timestamp betaAccessBypassTime) {
    this.betaAccessBypassTime = betaAccessBypassTime;
  }

  @Column(name = "email_verification_completion_time")
  public Timestamp getEmailVerificationCompletionTime() {return emailVerificationCompletionTime; }

  public void setEmailVerificationCompletionTime(Timestamp emailVerificationCompletionTime) {
    this.emailVerificationCompletionTime = emailVerificationCompletionTime;
  }

  @Column(name = "email_verification_bypass_time")
  public Timestamp getEmailVerificationBypassTime() {return emailVerificationBypassTime; }

  public void setEmailVerificationBypassTime(Timestamp emailVerificationBypassTime) {
    this.emailVerificationBypassTime = emailVerificationBypassTime;
  }

  @Column(name = "era_commons_bypass_time")
  public Timestamp getEraCommonsBypassTime() {return eraCommonsBypassTime; }

  public void setEraCommonsBypassTime(Timestamp eraCommonsBypassTime) {
    this.eraCommonsBypassTime = eraCommonsBypassTime;
  }

  @Column(name = "id_verification_completion_time")
  public Timestamp getIdVerificationCompletionTime() {return idVerificationCompletionTime;}

  public void setIdVerificationCompletionTime(Timestamp idVerificationCompletionTime) {
    this.idVerificationCompletionTime = idVerificationCompletionTime;
  }

  @Column(name = "id_verification_bypass_time")
  public Timestamp getIdVerificationBypassTime() {return idVerificationBypassTime; }

  public void setIdVerificationBypassTime(Timestamp idVerificationBypassTime) {
    this.idVerificationBypassTime = idVerificationBypassTime;
  }

  @Column(name = "two_factor_auth_completion_time")
  public Timestamp getTwoFactorAuthCompletionTime() {return twoFactorAuthCompletionTime; }

  public void setTwoFactorAuthCompletionTime(Timestamp twoFactorAuthCompletionTime) {
    this.twoFactorAuthCompletionTime = twoFactorAuthCompletionTime;
  }

  @Column(name = "two_factor_auth_bypass_time")
  public Timestamp getTwoFactorAuthBypassTime() {return twoFactorAuthBypassTime; }

  public void setTwoFactorAuthBypassTime(Timestamp twoFactorAuthBypassTime) {
    this.twoFactorAuthBypassTime = twoFactorAuthBypassTime;
  }

}
