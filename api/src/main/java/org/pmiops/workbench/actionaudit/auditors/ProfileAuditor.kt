package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.actionaudit.Agent
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.Profile

interface ProfileAuditor {
    fun fireCreateAction(createdProfile: Profile)

    fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile, agent: Agent)

    fun fireDeleteAction(userId: Long, userEmail: String)

    fun fireLoginAction(dbUser: DbUser)
}
