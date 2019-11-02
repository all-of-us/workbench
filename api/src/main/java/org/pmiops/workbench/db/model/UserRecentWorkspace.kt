package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_recent_workspace")
class UserRecentWorkspace {
    @get:Column(name = "last_access_date")
    var lastAccessDate: Timestamp? = null
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    private var userId: Long? = null
    private var workspaceId: Long? = null

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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as UserRecentWorkspace?
        return (id == that!!.id
                && lastAccessDate!!.equals(that.lastAccessDate)
                && userId == that.userId
                && workspaceId == that.workspaceId)
    }

    override fun hashCode(): Int {
        return Objects.hash(lastAccessDate, id, userId, workspaceId)
    }

    constructor() {}

    constructor(workspaceId: Long, userId: Long, lastAccessDate: Timestamp) {
        this.workspaceId = workspaceId
        this.userId = userId
        this.lastAccessDate = lastAccessDate
    }
}
