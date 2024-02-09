package org.pmiops.workbench.actionaudit.auditors;

public record CommonAuditEventInfo(
    String actionId, long userId, String userEmail, long timestamp) {}
