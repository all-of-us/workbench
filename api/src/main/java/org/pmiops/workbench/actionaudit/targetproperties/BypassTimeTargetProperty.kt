package org.pmiops.workbench.actionaudit.targetproperties

enum class BypassTimeTargetProperty
constructor(override val propertyName: String): SimpleTargetProperty {
    DATA_USE_AGREEMENT_BYPASS_TIME("data_use_agreement"),
    COMPLIANCE_TRAINING_BYPASS_TIME("compliance_training_time"),
    BETA_ACCESS_BYPASS_TIME("beta_access_bypass_time"),
    EMAIL_VERIFICATION_BYPASS_TIME("email_verification_bypass_time"),
    ERA_COMMONS_BYPASS_TIME("era_commons_bypass_time"),
    ID_VERIFICATION_BYPASS_TIME("id_verification_bypass_time"),
    TWO_FACTOR_AUTH_BYPASS_TIME("two_factor_auth_bypass_time"),

}
