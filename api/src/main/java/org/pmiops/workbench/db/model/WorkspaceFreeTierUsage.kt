package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "workspace_free_tier_usage")
class WorkspaceFreeTierUsage {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "workspace_free_tier_usage_id")
    var id: Long = 0
    @get:ManyToOne
    @get:JoinColumn(name = "user_id")
    var user: User? = null
    @get:OneToOne
    @get:JoinColumn(name = "workspace_id")
    var workspace: Workspace? = null
    @get:Column(name = "cost")
    var cost: Double = 0.toDouble()
        set(cost) {
            field = cost
            setLastUpdateTime()
        }
    @get:Column(name = "last_update_time")
    var lastUpdateTime: Timestamp? = null
        private set

    constructor() {}

    constructor(workspace: Workspace) {
        this.user = workspace.creator
        this.workspace = workspace
    }

    private fun setLastUpdateTime() {
        this.lastUpdateTime = Timestamp(Instant.now().toEpochMilli())
    }
}
