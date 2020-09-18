package org.pmiops.workbench.actionaudit.auditors

interface BillingProjectAuditor {
    fun fireDeleteAction(billingProjectName: String)
}
