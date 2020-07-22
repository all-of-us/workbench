package org.pmiops.workbench.actionaudit.auditors

interface FreeTierAuditor {
    fun fireFreeTierDollarQuotaAction(targetUserId: Long, previousDollarQuota: Double?, newDollarQuota: Double?)
}
