package org.pmiops.workbench.db.dao

import java.util.Optional
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.springframework.data.repository.CrudRepository

interface UserRecentWorkspaceDao : CrudRepository<UserRecentWorkspace, Long> {
    fun findByUserIdOrderByLastAccessDateDesc(userId: Long): List<UserRecentWorkspace>

    fun findFirstByWorkspaceIdAndUserId(workspaceId: Long, userId: Long): Optional<UserRecentWorkspace>

    fun deleteByUserIdAndWorkspaceIdIn(userId: Long, ids: Collection<Long>)
}
