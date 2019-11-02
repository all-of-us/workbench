package org.pmiops.workbench.db.model

import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "user_recent_resource")
class UserRecentResource {
    @get:Column(name = "lastAccessDate")
    var lastAccessDate: Timestamp? = null
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
    /**
     * TODO: Rename this column to reflect reality.
     *
     * @return the full GCS URI for the notebook, not just the notebook name as implied
     */
    @get:Column(name = "notebook_name")
    var notebookName: String? = null
    private var userId: Long? = null
    private var workspaceId: Long? = null
    @get:ManyToOne
    @get:JoinColumn(name = "cohort_id")
    var cohort: Cohort? = null
    @get:ManyToOne
    @get:JoinColumn(name = "concept_set_id")
    var conceptSet: ConceptSet? = null

    @Column(name = "user_id")
    fun getUserId(): Long {
        return userId!!
    }

    fun setUserId(userId: Long?) {
        this.userId = userId
    }

    @Column(name = "workspace_id")
    fun getWorkspaceId(): Long {
        return workspaceId!!
    }

    fun setWorkspaceId(workspaceId: Long) {
        this.workspaceId = workspaceId
    }

    constructor() {}

    constructor(
            workspaceId: Long, userId: Long, notebookName: String, lastAccessDate: Timestamp) {
        this.workspaceId = workspaceId
        this.userId = userId
        this.notebookName = notebookName
        this.lastAccessDate = lastAccessDate
        this.cohort = null
        this.conceptSet = null
    }

    constructor(workspaceId: Long, userId: Long, lastAccessDate: Timestamp) {
        this.workspaceId = workspaceId
        this.userId = userId
        this.notebookName = null
        this.lastAccessDate = lastAccessDate
    }
}
