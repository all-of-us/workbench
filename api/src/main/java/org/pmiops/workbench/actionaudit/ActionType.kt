package org.pmiops.workbench.actionaudit

// do not rename these, as they are serialized as text
enum class ActionType {
    LOGIN,
    LOGOUT,
    BYPASS,
    EDIT,
    CREATE,
    DUPLICATE_FROM,
    DUPLICATE_TO,
    COLLABORATE,
    DELETE,
    HIGH_EGRESS,
    COMMENT
}
