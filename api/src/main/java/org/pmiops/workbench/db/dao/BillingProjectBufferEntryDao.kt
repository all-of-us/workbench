package org.pmiops.workbench.db.dao

import java.sql.Timestamp
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BillingProjectBufferEntryDao : CrudRepository<BillingProjectBufferEntry, Long> {

    @get:Query("SELECT COUNT(*) FROM BillingProjectBufferEntry WHERE status IN (0, 2)")
    val currentBufferSize: Long?

    fun findByFireCloudProjectName(fireCloudProjectName: String): BillingProjectBufferEntry

    fun findAllByStatusAndLastStatusChangedTimeLessThan(
            status: Short, timestamp: Timestamp): List<BillingProjectBufferEntry>

    fun findFirstByStatusOrderByLastSyncRequestTimeAsc(status: Short): BillingProjectBufferEntry

    fun findFirstByStatusOrderByCreationTimeAsc(status: Short): BillingProjectBufferEntry

    fun countByStatus(status: Short): Long?

    @Query(value = "SELECT GET_LOCK('$ASSIGNING_LOCK', 1)", nativeQuery = true)
    fun acquireAssigningLock(): Int

    @Query(value = "SELECT RELEASE_LOCK('$ASSIGNING_LOCK')", nativeQuery = true)
    fun releaseAssigningLock(): Int

    // get Billing Projects which are ASSIGNED to Workspaces which have been DELETED and have NEW
    // Billing Migration Status.  These are ready to be garbage-collected
    fun findBillingProjectsForGarbageCollection(): List<String> {
        return findByStatusAndActiveStatusAndBillingMigrationStatus(
                StorageEnums.billingProjectBufferStatusToStorage(BillingProjectBufferStatus.ASSIGNED)!!,
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.DELETED)!!,
                StorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.NEW)!!)
    }

    @Query("SELECT p.fireCloudProjectName "
            + "FROM BillingProjectBufferEntry p "
            + "JOIN Workspace w "
            + "ON w.workspaceNamespace = p.fireCloudProjectName "
            + "AND p.status = :billingStatus "
            + "AND w.activeStatus = :workspaceStatus "
            + "AND w.billingMigrationStatus = :migrationStatus")
    fun findByStatusAndActiveStatusAndBillingMigrationStatus(
            @Param("billingStatus") billingStatus: Short,
            @Param("workspaceStatus") workspaceStatus: Short,
            @Param("migrationStatus") migrationStatus: Short): List<String>

    companion object {

        val ASSIGNING_LOCK = "ASSIGNING_LOCK"
    }
}
