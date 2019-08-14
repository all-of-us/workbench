package org.pmiops.workbench.db.model

import org.pmiops.workbench.billing.BillingProjectBufferStatus
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_project_buffer_entry_id")
    var id: Long = 0

    @Column(name = "firecloud_project_name")
    var fireCloudProjectName: String? = null

    @Column(name = "creation_time")
    var creationTime: Timestamp? = null

    @Column(name = "last_sync_request_time")
    var lastSyncRequestTime: Timestamp? = null

    @Column(name = "last_status_changed_time")
    var lastStatusChangedTime: Timestamp? = null
        private set

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    var assignedUser: User? = null

    @Column(name = "status")
    private var status: Short? = null

    fun getStatusEnum(): BillingProjectBufferStatus {
        return StorageEnums.billingProjectBufferStatusFromStorage(status)
    }

    fun setStatusEnum(
            status: BillingProjectBufferStatus, currentTimestamp: Supplier<Timestamp>) {
        this.lastStatusChangedTime = currentTimestamp.get()
        this.status = StorageEnums.billingProjectBufferStatusToStorage(status)
    }
}
