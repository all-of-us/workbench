package org.pmiops.workbench.actionaudit

// do not rename these, as they are serialized as text
enum class AuditColumn {
    TIMESTAMP,
    ACTION_ID,
    ACTION_TYPE,
    AGENT_TYPE,
    AGENT_ID,
    AGENT_EMAIL,
    TARGET_TYPE,
    TARGET_ID,
    TARGET_PROPERTY,
    PREV_VALUE,
    NEW_VALUE
}
