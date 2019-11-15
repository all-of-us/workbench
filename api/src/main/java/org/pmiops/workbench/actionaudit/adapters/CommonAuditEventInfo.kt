package org.pmiops.workbench.actionaudit.adapters

data class CommonAuditEventInfo(
        val actionId: String,
        val userId: Long,
        val userEmail: String?,
        val timestamp: Long)
