package org.pmiops.workbench.actionaudit.targetproperties

enum class AccountTargetProperty
    constructor(override val propertyName: String) : SimpleTargetProperty {
    IS_ENABLED("is_enabled"),
    REGISTRATION_STATUS("registration_status"),
    ACKNOWLEDGED_TOS_VERSION("acknowledged_tos_version"),
    FREE_TIER_DOLLAR_QUOTA("free_tier_dollar_quota"),
    ACCESS_TIERS("access_tiers");
}
