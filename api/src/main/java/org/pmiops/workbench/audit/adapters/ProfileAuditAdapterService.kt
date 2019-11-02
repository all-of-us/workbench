package org.pmiops.workbench.audit.adapters

import org.pmiops.workbench.model.Profile

interface ProfileAuditAdapterService {
    fun fireCreateAction(createdProfile: Profile)

    fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile)

    fun fireDeleteAction(userId: Long, userEmail: String)
}
