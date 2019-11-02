package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.model.BillingStatus
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 *
 * Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
interface WorkspaceDao : CrudRepository<Workspace, Long> {

    fun findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            workspaceNamespace: String, firecloudName: String, activeStatus: Short): Workspace

    fun findByWorkspaceNamespaceAndNameAndActiveStatus(
            workspaceNamespace: String, name: String, activeStatus: Short): Workspace

    @Query("SELECT distinct w.workspaceNamespace, w from Workspace w")
    fun findAllWorkspaceNamespaces(): Set<String>

    @Query("SELECT w FROM Workspace w LEFT JOIN FETCH w.cohorts c LEFT JOIN FETCH c.cohortReviews"
            + " WHERE w.workspaceNamespace = (:ns) AND w.firecloudName = (:fcName)"
            + " AND w.activeStatus = (:status)")
    fun findByFirecloudNameAndActiveStatusWithEagerCohorts(
            @Param("ns") workspaceNamespace: String,
            @Param("fcName") fcName: String,
            @Param("status") status: Short): Workspace

    fun findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested(): List<Workspace>

    fun findAllByFirecloudUuidIn(firecloudUuids: Collection<String>): List<Workspace>

    fun findAllByWorkspaceIdIn(dbIds: Collection<Long>): List<Workspace>

    fun findAllByBillingMigrationStatus(billingMigrationStatus: Short?): List<Workspace>

    fun findAllByBillingMigrationStatus(status: BillingMigrationStatus): List<Workspace> {
        return findAllByBillingMigrationStatus(StorageEnums.billingMigrationStatusToStorage(status))
    }

    fun updateBillingStatus(workspaceId: Long, status: BillingStatus) {
        val toUpdate = findOne(workspaceId)
        toUpdate.billingStatus = status
        save(toUpdate)
    }
}
