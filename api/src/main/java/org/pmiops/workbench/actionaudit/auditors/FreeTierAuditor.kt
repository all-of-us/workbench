package org.pmiops.workbench.actionaudit.auditors

interface FreeTierAuditor {
    fun fireFreeTierDollarQuotaAction(userId: Long, previousDollarQuota: Double?, newDollarQuota: Double?)
}
