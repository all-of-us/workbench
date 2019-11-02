package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.function.Supplier
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient

@Entity
@Table(name = "billing_project_buffer_entry")
class BillingProjectBufferEntry {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "billing_project_buffer_entry_id")
    var id: Long = 0
    @get:Column(name = "firecloud_project_name")
    var fireCloudProjectName: String? = null
    @get:Column(name = "creation_time")
    var creationTime: Timestamp? = null
    @get:Column(name = "last_sync_request_time")
    var lastSyncRequestTime: Timestamp? = null
    @get:Column(name = "last_status_changed_time")
    var lastStatusChangedTime: Timestamp? = null
        private set
    private var status: Short? = null
    @get:ManyToOne
    @get:JoinColumn(name = "assigned_user_id")
    var assignedUser: User? = null

    val statusEnum: BillingProjectBufferStatus
        @Transient
        get() = StorageEnums.billingProjectBufferStatusFromStorage(status)

    enum class BillingProjectBufferStatus {
        // Sent a request to FireCloud to create a BillingProject. Status of BillingProject is TBD
        CREATING,

        ERROR, // Failed to create BillingProject
        AVAILABLE, // BillingProject is ready to be assigned to a user
        ASSIGNING, //  BillingProject is being assigned to a user
        ASSIGNED, // BillingProject has been assigned to a user

        // The ownership of this project has been transferred from the AoU App Engine SA
        // to an alternate SA, to help ensure that the AoU App Engine SA is not a member of too many
        // groups. See https://precisionmedicineinitiative.atlassian.net/browse/RW-3435
        GARBAGE_COLLECTED
    }

    fun setStatusEnum(
            status: BillingProjectBufferStatus, currentTimestamp: Supplier<Timestamp>) {
        this.lastStatusChangedTime = currentTimestamp.get()
        this.status = StorageEnums.billingProjectBufferStatusToStorage(status)
    }

    @Column(name = "status")
    private fun getStatus(): Short {
        return this.status!!
    }

    private fun setStatus(s: Short) {
        this.status = s
    }
}
