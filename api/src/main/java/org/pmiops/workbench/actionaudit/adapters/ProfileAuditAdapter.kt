package org.pmiops.workbench.actionaudit.adapters

import org.pmiops.workbench.model.Profile

interface ProfileAuditAdapter {
    fun fireCreateAction(createdProfile: Profile)

    fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile)

    fun fireDeleteAction(userId: Long, userEmail: String)
}
