package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.HashSet
import javax.persistence.CascadeType
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
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.SpecificPopulationEnum
import org.pmiops.workbench.model.WorkspaceActiveStatus

@Entity
@Table(name = "workspace")
class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workspace_id")
    var workspaceId: Long = 0

    @Column(name = "firecloud_uuid")
    var firecloudUuid: String? = null

    val firecloudWorkspaceId: FirecloudWorkspaceId
        @Transient
        get() = FirecloudWorkspaceId(workspaceNamespace!!, firecloudName!!)

    @Version
    @Column(name = "version")
    var version: Int = 0

    @Column(name = "name")
    var name: String? = null

    @Column(name = "workspace_namespace")
    var workspaceNamespace: String? = null

    @Column(name = "firecloud_name")
    var firecloudName: String? = null

    @ManyToOne
    @JoinColumn(name = "cdr_version_id")
    var cdrVersion: CdrVersion? = null

    @ManyToOne
    @JoinColumn(name = "creator_id")
    var creator: User? = null

    @Column(name = "creation_time")
    var creationTime: Timestamp? = null

    @Column(name = "last_modified_time")
    var lastModifiedTime: Timestamp? = null

    @Column(name = "last_accessed_time")
    var lastAccessedTime: Timestamp? = null

    @Column(name = "published")
    var published: Boolean = false

    @Column(name = "rp_disease_focused_research")
    var diseaseFocusedResearch: Boolean = false

    @Column(name = "rp_disease_of_focus")
    var diseaseOfFocus: String? = null

    @Column(name = "rp_methods_development")
    var methodsDevelopment: Boolean = false

    @Column(name = "rp_control_set")
    var controlSet: Boolean = false

    @Column(name = "rp_ancestry")
    var ancestry: Boolean = false

    @Column(name = "rp_commercial_purpose")
    var commercialPurpose: Boolean = false

    @Column(name = "rp_population")
    var population: Boolean = false

    @Column(name = "rp_social_behavioral")
    var socialBehavioral: Boolean = false

    @Column(name = "rp_population_health")
    var populationHealth: Boolean = false

    @Column(name = "rp_educational")
    var educational: Boolean = false

    @Column(name = "rp_drug_development")
    var drugDevelopment: Boolean = false

    @Column(name = "rp_other_purpose")
    var otherPurpose: Boolean = false

    @Column(name = "rp_other_purpose_details")
    var otherPurposeDetails: String? = null

    @Column(name = "rp_other_population_details")
    var otherPopulationDetails: String? = null

    @Column(name = "rp_additional_notes")
    var additionalNotes: String? = null

    @Column(name = "rp_reason_for_all_of_us")
    var reasonForAllOfUs: String? = null

    @Column(name = "rp_intended_study")
    var intendedStudy: String? = null

    @Column(name = "rp_anticipated_findings")
    var anticipatedFindings: String? = null

    @Column(name = "rp_review_requested")
    var reviewRequested: Boolean? = null
        set(reviewRequested) {
            if (reviewRequested != null && reviewRequested && this.timeRequested == null) {
                this.timeRequested = Timestamp(System.currentTimeMillis())
            }
            field = reviewRequested
        }

    @Column(name = "rp_approved")
    var approved: Boolean? = null

    @Column(name = "rp_time_requested")
    var timeRequested: Timestamp? = null

    @Column(name = "data_access_level")
    private var dataAccessLevel: Short = 0

    fun getDataAccessLevelEnum(): DataAccessLevel {
        return CommonStorageEnums.dataAccessLevelFromStorage(dataAccessLevel)
    }

    fun setDataAccessLevelEnum(dataAccessLevel: DataAccessLevel) {
        this.dataAccessLevel = CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel)
    }

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @Column(name = "specific_population")
    private var populationDetails: Set<Short> = HashSet()

    fun getSpecificPopulationsEnum(): Set<SpecificPopulationEnum> {
        return populationDetails.map { s -> StorageEnums.specificPopulationFromStorage(s) }.toSet()
    }

    fun setSpecificPopulationsEnum(enums: Set<SpecificPopulationEnum>) {
        populationDetails = enums.map { s -> StorageEnums.specificPopulationToStorage(s) }.toSet()
    }

    @Column(name = "active_status")
    private var activeStatus: Short = 0

    fun getWorkspaceActiveStatusEnum(): WorkspaceActiveStatus {
        return StorageEnums.workspaceActiveStatusFromStorage(activeStatus)
    }

    fun setWorkspaceActiveStatusEnum(activeStatus: WorkspaceActiveStatus) {
        this.activeStatus = StorageEnums.workspaceActiveStatusToStorage(activeStatus)
    }

    @Column(name = "billing_migration_status")
    private var billingMigrationStatus: Short = 0

    fun getBillingMigrationStatusEnum(): BillingMigrationStatus {
        return StorageEnums.billingMigrationStatusFromStorage(billingMigrationStatus)
    }

    fun setBillingMigrationStatusEnum(status: BillingMigrationStatus) {
        this.billingMigrationStatus = StorageEnums.billingMigrationStatusToStorage(status)
    }

    @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    var cohorts: MutableSet<Cohort> = HashSet()
    set(value) {
        cohorts.clear()
        cohorts.addAll(value)
    }

    @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    var dataSets: MutableSet<DataSet> = HashSet()
    set(value) {
        dataSets.clear()
        dataSets.addAll(value)
    }

    @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = [CascadeType.ALL])
    var conceptSets: MutableSet<ConceptSet> = mutableSetOf()
    set(value) {
        field.clear()
        field.addAll(value)
    }
}
