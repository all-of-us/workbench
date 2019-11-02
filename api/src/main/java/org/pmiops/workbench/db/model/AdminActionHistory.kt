package org.pmiops.workbench.db.model

import java.sql.Timestamp
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "admin_action_history")
class AdminActionHistory {
    @get:Id
    @get:Column(name = "history_id")
    var historyId: Long = 0
    @get:Column(name = "admin_user_id")
    var adminUserId: Long = 0
    @get:Column(name = "target_user_id")
    var targetUserId: Long? = null
    @get:Column(name = "target_workspace_id")
    var targetWorkspaceId: Long? = null
    @get:Column(name = "target_action")
    var targetAction: String? = null
    @get:Column(name = "old_value_as_string")
    var oldValue: String? = null
    @get:Column(name = "new_value_as_string")
    var newValue: String? = null
    @get:Column(name = "timestamp")
    var timestamp: Timestamp? = null

    fun setTimestamp() {
        this.timestamp = Timestamp(Instant.now().toEpochMilli())
    }
}
