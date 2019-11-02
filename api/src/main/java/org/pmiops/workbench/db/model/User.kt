package org.pmiops.workbench.db.model

import com.google.gson.Gson
import java.sql.Timestamp
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Collectors
import javax.persistence.CascadeType
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderColumn
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.EmailVerificationStatus

@Entity
@Table(name = "user")
class User {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "user_id")
    var userId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    // A nonce which can be used during the account creation flow to verify
    // unauthenticated API calls after account creation, but before initial login.
    @get:Column(name = "creation_nonce")
    var creationNonce: Long? = null
    // The Google email address that the user signs in with.
    @get:Column(name = "email")
    var email: String? = null
    // The email address that can be used to contact the user.
    @get:Column(name = "contact_email")
    var contactEmail: String? = null
    @get:Column(name = "data_access_level")
    var dataAccessLevel: Short? = null
    @get:Column(name = "given_name")
    var givenName: String? = null
    @get:Column(name = "family_name")
    var familyName: String? = null
    // TODO: consider dropping this (do we want researcher phone numbers?)
    @get:Column(name = "phone_number")
    var phoneNumber: String? = null
    @get:Column(name = "current_position")
    var currentPosition: String? = null
    @get:Column(name = "organization")
    var organization: String? = null
    @get:Column(name = "free_tier_credits_limit_override")
    var freeTierCreditsLimitOverride: Double? = null
    @get:Column(name = "first_sign_in_time")
    var firstSignInTime: Timestamp? = null
    // Authorities (special permissions) are granted using api/project.rb set-authority.
    @get:ElementCollection(fetch = FetchType.LAZY)
    @get:CollectionTable(name = "authority", joinColumns = [JoinColumn(name = "user_id")])
    @get:Column(name = "authority")
    var authorities: Set<Short>? = HashSet()
    @get:Column(name = "id_verification_is_valid")
    var idVerificationIsValid: Boolean? = null
    @get:Column(name = "demographic_survey_completion_time")
    var demographicSurveyCompletionTime: Timestamp? = null
    @get:Column(name = "disabled")
    var disabled: Boolean = false
    @get:Column(name = "email_verification_status")
    var emailVerificationStatus: Short? = null
    @get:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "user")
    @get:Column(name = "page_id")
    var pageVisits: Set<PageVisit> = HashSet()
    @get:Column(name = "cluster_config_default")
    var clusterConfigDefaultRaw: String? = null

    private var institutionalAffiliations: MutableList<InstitutionalAffiliation> = ArrayList()
    @get:Column(name = "about_you")
    var aboutYou: String? = null
    @get:Column(name = "area_of_research")
    var areaOfResearch: String? = null
    @get:Column(name = "cluster_create_retries")
    var clusterCreateRetries: Int? = null
    @get:Column(name = "billing_project_retries")
    var billingProjectRetries: Int? = null
    @get:Column(name = "moodle_id")
    var moodleId: Int? = null

    // Access module fields go here. See http://broad.io/aou-access-modules for docs.
    @get:Column(name = "era_commons_linked_nih_username")
    var eraCommonsLinkedNihUsername: String? = null
    @get:Column(name = "era_commons_link_expire_time")
    var eraCommonsLinkExpireTime: Timestamp? = null
    @get:Column(name = "era_commons_completion_time")
    var eraCommonsCompletionTime: Timestamp? = null
    @get:Column(name = "beta_access_request_time")
    var betaAccessRequestTime: Timestamp? = null
    @get:Column(name = "beta_access_bypass_time")
    var betaAccessBypassTime: Timestamp? = null
    @get:Column(name = "data_use_agreement_completion_time")
    var dataUseAgreementCompletionTime: Timestamp? = null
    @get:Column(name = "data_use_agreement_bypass_time")
    var dataUseAgreementBypassTime: Timestamp? = null
    @get:Column(name = "data_use_agreement_signed_version")
    var dataUseAgreementSignedVersion: Int? = null
    @get:Column(name = "compliance_training_completion_time")
    var complianceTrainingCompletionTime: Timestamp? = null
    @get:Column(name = "compliance_training_bypass_time")
    var complianceTrainingBypassTime: Timestamp? = null
    @get:Column(name = "compliance_training_expiration_time")
    var complianceTrainingExpirationTime: Timestamp? = null
    @get:Column(name = "era_commons_bypass_time")
    var eraCommonsBypassTime: Timestamp? = null
    @get:Column(name = "email_verification_completion_time")
    var emailVerificationCompletionTime: Timestamp? = null
    @get:Column(name = "email_verification_bypass_time")
    var emailVerificationBypassTime: Timestamp? = null
    @get:Column(name = "id_verification_completion_time")
    var idVerificationCompletionTime: Timestamp? = null
    @get:Column(name = "id_verification_bypass_time")
    var idVerificationBypassTime: Timestamp? = null
    @get:Column(name = "two_factor_auth_completion_time")
    var twoFactorAuthCompletionTime: Timestamp? = null
    @get:Column(name = "two_factor_auth_bypass_time")
    var twoFactorAuthBypassTime: Timestamp? = null
    @get:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "user")
    var demographicSurvey: DemographicSurvey? = null
    @get:OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "user")
    var address: Address? = null

    var dataAccessLevelEnum: DataAccessLevel
        @Transient
        get() = CommonStorageEnums.dataAccessLevelFromStorage(dataAccessLevel)
        set(dataAccessLevel) {
            dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel)
        }

    var authoritiesEnum: Set<Authority>?
        @Transient
        get() {
            val from = authorities ?: return null
            return from.stream().map(Function<Short, R> { StorageEnums.authorityFromStorage(it) }).collect(Collectors.toSet<Any>())
        }
        set(newAuthorities) {
            this.authorities = newAuthorities.stream().map<Short>(Function<Authority, Short> { StorageEnums.authorityToStorage(it) }).collect<Set<Short>, Any>(Collectors.toSet())
        }

    var clusterConfigDefault: ClusterConfig?
        @Transient
        get() = if (clusterConfigDefaultRaw == null) {
            null
        } else Gson().fromJson(clusterConfigDefaultRaw, ClusterConfig::class.java)
        set(value) {
            var rawValue: String? = null
            if (value != null) {
                rawValue = Gson().toJson(value)
            }
            clusterConfigDefaultRaw = rawValue
        }

    var emailVerificationStatusEnum: EmailVerificationStatus
        @Transient
        get() = StorageEnums.emailVerificationStatusFromStorage(emailVerificationStatus)
        set(emailVerificationStatus) {
            emailVerificationStatus = StorageEnums.emailVerificationStatusToStorage(emailVerificationStatus)
        }

    /**
     * This is a Gson compatible class for encoding a JSON blob which is stored in MySQL. This
     * represents cluster configuration overrides we support on a per-user basis for their notebook
     * cluster. Corresponds to Leonardo's MachineConfig model. All fields are optional.
     *
     *
     * Any changes to this class should produce backwards-compatible JSON.
     */
    class ClusterConfig {
        // Master persistent disk size in GB.
        var masterDiskSize: Int? = null
        // GCE machine type, e.g. n1-standard-2.
        var machineType: String? = null
    }

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "user", cascade = [CascadeType.ALL])
    @OrderColumn(name = "order_index")
    fun getInstitutionalAffiliations(): List<InstitutionalAffiliation> {
        return institutionalAffiliations
    }

    fun setInstitutionalAffiliations(
            newInstitutionalAffiliations: MutableList<InstitutionalAffiliation>) {
        this.institutionalAffiliations = newInstitutionalAffiliations
    }

    fun clearInstitutionalAffiliations() {
        this.institutionalAffiliations.clear()
    }

    fun addInstitutionalAffiliation(newInstitutionalAffiliation: InstitutionalAffiliation) {
        this.institutionalAffiliations.add(newInstitutionalAffiliation)
    }
}
