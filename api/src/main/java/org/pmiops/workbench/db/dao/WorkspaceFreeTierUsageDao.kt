package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceFreeTierUsageDao : CrudRepository<WorkspaceFreeTierUsage, Long> {

    fun findOneByWorkspace(workspace: Workspace): WorkspaceFreeTierUsage

    fun updateCost(workspace: Workspace, cost: Double) {
        var usage: WorkspaceFreeTierUsage? = findOneByWorkspace(workspace)
        if (usage == null) {
            usage = WorkspaceFreeTierUsage(workspace)
        }
        usage.cost = cost
        save(usage)
    }

    @Query("SELECT SUM(cost) FROM WorkspaceFreeTierUsage u WHERE user = :user")
    fun totalCostByUser(@Param("user") user: User): Double?
}
