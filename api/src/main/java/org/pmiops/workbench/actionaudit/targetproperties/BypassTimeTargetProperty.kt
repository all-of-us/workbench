package org.pmiops.workbench.actionaudit.targetproperties

enum class BypassTimeTargetProperty
constructor(override val propertyName: String) : SimpleTargetProperty {
    DATA_USE_AGREEMENT_BYPASS_TIME("data_use_agreement_bypass_time"),
    COMPLIANCE_TRAINING_BYPASS_TIME("compliance_training_time"),
    ERA_COMMONS_BYPASS_TIME("era_commons_bypass_time"),
    TWO_FACTOR_AUTH_BYPASS_TIME("two_factor_auth_bypass_time"),
    RAS_LINK_LOGIN_GOV("ras_link_login_gov_bypass_time")
}
