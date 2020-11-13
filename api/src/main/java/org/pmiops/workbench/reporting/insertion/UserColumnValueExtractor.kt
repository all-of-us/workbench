package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryParameterValue
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.InstitutionalRole
import org.pmiops.workbench.model.ReportingUser
import java.util.function.Function

/*
* BigQuery user table columns. This list contains some values that are in other tables
* but joined in the user projection, and omits others that are not exposed in the analytics
* dataset.
*/
enum class UserColumnValueExtractor(
        override val parameterName: String,
        override val rowToInsertValueFunction: Function<ReportingUser, Any?>,
        override val queryParameterValueFunction: Function<ReportingUser, QueryParameterValue?>) : ColumnValueExtractor<ReportingUser?> {
    ABOUT_YOU("about_you", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.aboutYou }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.aboutYou) }), AREA_OF_RESEARCH(
            "area_of_research", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.areaOfResearch }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.areaOfResearch) }),
    COMPLIANCE_TRAINING_BYPASS_TIME(
            "compliance_training_bypass_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.complianceTrainingBypassTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.complianceTrainingBypassTime) }),
    COMPLIANCE_TRAINING_COMPLETION_TIME(
            "compliance_training_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.complianceTrainingCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.complianceTrainingCompletionTime) }),
    COMPLIANCE_TRAINING_EXPIRATION_TIME(
            "compliance_training_expiration_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.complianceTrainingExpirationTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.complianceTrainingExpirationTime) }),
    CONTACT_EMAIL("contact_email", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.contactEmail }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.contactEmail) }), CREATION_TIME(
            "creation_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.creationTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.creationTime) }),
    CURRENT_POSITION(
            "current_position", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.currentPosition }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.currentPosition) }),
    DATA_ACCESS_LEVEL(
            "data_access_level",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.enumToString<DataAccessLevel>(u.dataAccessLevel) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.enumToQpv<DataAccessLevel>(u.dataAccessLevel) }),
    DATA_USE_AGREEMENT_BYPASS_TIME(
            "data_use_agreement_bypass_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.dataUseAgreementBypassTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.dataUseAgreementBypassTime) }),
    DATA_USE_AGREEMENT_COMPLETION_TIME(
            "data_use_agreement_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.dataUseAgreementCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.dataUseAgreementCompletionTime) }),
    DATA_USE_AGREEMENT_SIGNED_VERSION(
            "data_use_agreement_signed_version", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.dataUseAgreementSignedVersion },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.int64(u.dataUseAgreementSignedVersion) }),
    DEMOGRAPHIC_SURVEY_COMPLETION_TIME(
            "demographic_survey_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.demographicSurveyCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.demographicSurveyCompletionTime) }),
    DISABLED("disabled", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.disabled }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.bool(u.disabled) }), ERA_COMMONS_BYPASS_TIME(
            "era_commons_bypass_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.eraCommonsBypassTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.eraCommonsBypassTime) }),
    ERA_COMMONS_COMPLETION_TIME(
            "era_commons_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.eraCommonsCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.eraCommonsCompletionTime) }),
    FAMILY_NAME("family_name", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.familyName }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.familyName) }), FIRST_REGISTRATION_COMPLETION_TIME(
            "first_registration_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.firstRegistrationCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.firstRegistrationCompletionTime) }),
    FIRST_SIGN_IN_TIME(
            "first_sign_in_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.firstSignInTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.firstSignInTime) }),
    FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE(
            "free_tier_credits_limit_days_override", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.freeTierCreditsLimitDaysOverride },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.int64(u.freeTierCreditsLimitDaysOverride) }),
    FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE(
            "free_tier_credits_limit_dollars_override", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.freeTierCreditsLimitDollarsOverride },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.float64(u.freeTierCreditsLimitDollarsOverride) }),
    GIVEN_NAME("given_name", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.givenName }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.givenName) }), LAST_MODIFIED_TIME(
            "last_modified_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.lastModifiedTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.lastModifiedTime) }),
    PROFESSIONAL_URL(
            "professional_url", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.professionalUrl }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.professionalUrl) }),
    TWO_FACTOR_AUTH_BYPASS_TIME(
            "two_factor_auth_bypass_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.twoFactorAuthBypassTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.twoFactorAuthBypassTime) }),
    TWO_FACTOR_AUTH_COMPLETION_TIME(
            "two_factor_auth_completion_time",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.toInsertRowString(u.twoFactorAuthCompletionTime) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.toTimestampQpv(u.twoFactorAuthCompletionTime) }),
    USER_ID("user_id", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.userId }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.int64(u.userId) }), USERNAME("username", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.username }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.username) }), CITY("city", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.city }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.city) }), COUNTRY("country", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.country }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.country) }), STATE("state", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.state }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.state) }), STREET_ADDRESS_1(
            "street_address_1", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.streetAddress1 }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.streetAddress1) }),
    STREET_ADDRESS_2(
            "street_address_2", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.streetAddress2 }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.streetAddress2) }),
    ZIP_CODE("zip_code", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.zipCode }, Function<ReportingUser, QueryParameterValue?> { a: ReportingUser -> QueryParameterValue.string(a.zipCode) }), INSTITUTION_ID(
            "institution_id", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.institutionId }, Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.int64(u.institutionId) }),
    INSTITUTIONAL_ROLE_ENUM(
            "institutional_role_enum",
            Function<ReportingUser, Any?> { u: ReportingUser -> QueryParameterValues.enumToString<InstitutionalRole>(u.institutionalRoleEnum) },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValues.enumToQpv<InstitutionalRole>(u.institutionalRoleEnum) }),
    INSTITUTIONAL_ROLE_OTHER_TEXT(
            "institutional_role_other_text", Function<ReportingUser, Any?> { obj: ReportingUser -> obj.institutionalRoleOtherText },
            Function<ReportingUser, QueryParameterValue?> { u: ReportingUser -> QueryParameterValue.string(u.institutionalRoleOtherText) });

    companion object {
        // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
        // as enums can't be abstract or extend abstract classes.
        const val TABLE_NAME = "user"
    }
}
