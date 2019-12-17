package org.pmiops.workbench.actionaudit.auditors

data class CommonAuditEventInfo(
    val actionId: String,
    val userId: Long,
    val userEmail: String?,
    val timestamp: Long
)
