package org.pmiops.workbench.actionaudit.adapters

import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.Profile

interface ProfileAuditAdapter : AuditAdapter<Profile> {
    fun fireCreateAction(createdProfile: Profile)

    fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile)

    fun fireDeleteAction(userId: Long, userEmail: String)

    fun fireLoginAction(dbUser: DbUser)
}
