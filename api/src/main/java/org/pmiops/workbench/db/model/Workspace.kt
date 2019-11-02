package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.HashSet
import java.util.Objects
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
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.Version
import org.pmiops.workbench.model.BillingAccountType
import org.pmiops.workbench.model.BillingStatus
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.SpecificPopulationEnum
import org.pmiops.workbench.model.WorkspaceActiveStatus

@Entity
@Table(name = "workspace")
class Workspace {
    @get:Column(name = "firecloud_uuid")
    var firecloudUuid: String? = null

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "workspace_id")
    var workspaceId: Long = 0
    @get:Version
    @get:Column(name = "version")
    var version: Int = 0
    @get:Column(name = "name")
    var name: String? = null
    @get:Column(name = "workspace_namespace")
    var workspaceNamespace: String? = null
    @get:Column(name = "firecloud_name")
    var firecloudName: String? = null
    @get:Column(name = "data_access_level")
    var dataAccessLevel: Short? = null
    @get:ManyToOne
    @get:JoinColumn(name = "cdr_version_id")
    var cdrVersion: CdrVersion? = null
    @get:ManyToOne
    @get:JoinColumn(name = "creator_id")
    var creator: User? = null
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null
    @get:Column(name = "last_accessed_time")
    var lastAccessedTime: Timestamp? = null
    private var cohorts: MutableSet<Cohort> = HashSet()
    @get:OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    var conceptSets: Set<ConceptSet> = HashSet()
    private var dataSets: MutableSet<DataSet> = HashSet()
    @get:Column(name = "active_status")
    private var activeStatus: Short? = null
    private var billingMigrationStatus = StorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.OLD)
    @get:Column(name = "published")
    var published: Boolean = false

    @get:Column(name = "rp_disease_focused_research")
    var diseaseFocusedResearch: Boolean = false
    @get:Column(name = "rp_disease_of_focus")
    var diseaseOfFocus: String? = null
    @get:Column(name = "rp_methods_development")
    var methodsDevelopment: Boolean = false
    @get:Column(name = "rp_control_set")
    var controlSet: Boolean = false
    @get:Column(name = "rp_ancestry")
    var ancestry: Boolean = false
    @get:Column(name = "rp_commercial_purpose")
    var commercialPurpose: Boolean = false
    @get:Column(name = "rp_population")
    var population: Boolean = false
    @get:ElementCollection(fetch = FetchType.LAZY)
    @get:CollectionTable(name = "specific_populations", joinColumns = [JoinColumn(name = "workspace_id")])
    @get:Column(name = "specific_population")
    var populationDetails: Set<Short>? = HashSet()
    @get:Column(name = "rp_social_behavioral")
    var socialBehavioral: Boolean = false
    @get:Column(name = "rp_population_health")
    var populationHealth: Boolean = false
    @get:Column(name = "rp_educational")
    var educational: Boolean = false
    @get:Column(name = "rp_drug_development")
    var drugDevelopment: Boolean = false
    @get:Column(name = "rp_other_purpose")
    var otherPurpose: Boolean = false
    @get:Column(name = "rp_other_purpose_details")
    var otherPurposeDetails: String? = null
    @get:Column(name = "rp_other_population_details")
    var otherPopulationDetails: String? = null
    @get:Column(name = "rp_additional_notes")
    var additionalNotes: String? = null
    @get:Column(name = "rp_reason_for_all_of_us")
    var reasonForAllOfUs: String? = null
    @get:Column(name = "rp_intended_study")
    var intendedStudy: String? = null
    @get:Column(name = "rp_anticipated_findings")
    var anticipatedFindings: String? = null

    @get:Column(name = "rp_review_requested")
    var reviewRequested: Boolean? = null
        set(reviewRequested) {
            if (reviewRequested != null && reviewRequested && this.timeRequested == null) {
                this.timeRequested = Timestamp(System.currentTimeMillis())
            }
            field = reviewRequested
        }
    @get:Column(name = "rp_approved")
    var approved: Boolean? = null
    @get:Column(name = "rp_time_requested")
    var timeRequested: Timestamp? = null
    private var billingStatus = StorageEnums.billingStatusToStorage(BillingStatus.ACTIVE)
    private var billingAccountType = StorageEnums.billingAccountTypeToStorage(BillingAccountType.FREE_TIER)

    var dataAccessLevelEnum: DataAccessLevel
        @Transient
        get() = CommonStorageEnums.dataAccessLevelFromStorage(dataAccessLevel)
        set(dataAccessLevel) {
            dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel)
        }

    var specificPopulationsEnum: Set<SpecificPopulationEnum>?
        @Transient
        get() {
            val from = populationDetails ?: return null
            return from.stream()
                    .map(Function<Short, R> { StorageEnums.specificPopulationFromStorage(it) })
                    .collect(Collectors.toSet<Any>())
        }
        set(newPopulationDetails) {
            populationDetails = newPopulationDetails.stream()
                    .map<Short>(Function<SpecificPopulationEnum, Short> { StorageEnums.specificPopulationToStorage(it) })
                    .collect<Set<Short>, Any>(Collectors.toSet())
        }

    val firecloudWorkspaceId: FirecloudWorkspaceId
        @Transient
        get() = FirecloudWorkspaceId(workspaceNamespace, firecloudName)

    var workspaceActiveStatusEnum: WorkspaceActiveStatus
        @Transient
        get() = StorageEnums.workspaceActiveStatusFromStorage(activeStatus)
        set(activeStatus) {
            activeStatus = StorageEnums.workspaceActiveStatusToStorage(activeStatus)
        }

    var billingMigrationStatusEnum: BillingMigrationStatus
        @Transient
        get() = StorageEnums.billingMigrationStatusFromStorage(billingMigrationStatus)
        set(status) {
            this.billingMigrationStatus = StorageEnums.billingMigrationStatusToStorage(status)
        }

    class FirecloudWorkspaceId(val workspaceNamespace: String, val workspaceName: String) {

        override fun hashCode(): Int {
            return Objects.hash(workspaceNamespace, workspaceName)
        }

        override fun equals(obj: Any?): Boolean {
            if (obj !is FirecloudWorkspaceId) {
                return false
            }
            val that = obj as FirecloudWorkspaceId?
            return this.workspaceNamespace == that!!.workspaceNamespace && this.workspaceName == that.workspaceName
        }
    }

    // see https://precisionmedicineinitiative.atlassian.net/browse/RW-2705
    enum class BillingMigrationStatus {
        OLD, // pre-1PPW; this billing project may be associated with multiple workspaces
        NEW, // a One Project Per Workspace (1PPW) billing project
        MIGRATED // a pre-1PPW billing project which has been cloned and is now ready to be deleted
    }

    init {
        workspaceActiveStatusEnum = WorkspaceActiveStatus.ACTIVE
    }

    @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    fun getCohorts(): Set<Cohort> {
        return cohorts
    }

    fun setCohorts(cohorts: MutableSet<Cohort>) {
        this.cohorts = cohorts
    }

    fun addCohort(cohort: Cohort) {
        this.cohorts.add(cohort)
    }

    @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    fun getDataSets(): Set<DataSet> {
        return dataSets
    }

    fun setDataSets(dataSets: MutableSet<DataSet>) {
        this.dataSets = dataSets
    }

    fun addDataSet(dataSet: DataSet) {
        this.dataSets.add(dataSet)
    }

    @Column(name = "billing_migration_status")
    private fun getBillingMigrationStatus(): Short {
        return this.billingMigrationStatus!!
    }

    private fun setBillingMigrationStatus(s: Short) {
        this.billingMigrationStatus = s
    }

    @Column(name = "billing_status")
    fun getBillingStatus(): BillingStatus {
        return StorageEnums.billingStatusFromStorage(billingStatus)
    }

    fun setBillingStatus(billingStatus: BillingStatus) {
        this.billingStatus = StorageEnums.billingStatusToStorage(billingStatus)
    }

    @Column(name = "billing_account_type")
    fun getBillingAccountType(): BillingAccountType {
        return StorageEnums.billingAccountTypeFromStorage(billingAccountType)
    }

    fun setBillingAccountType(billingAccountType: BillingAccountType) {
        this.billingAccountType = StorageEnums.billingAccountTypeToStorage(billingAccountType)
    }
}
