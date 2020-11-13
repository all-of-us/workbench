package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryParameterValue
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues
import org.pmiops.workbench.model.DuaType
import org.pmiops.workbench.model.OrganizationType
import org.pmiops.workbench.model.ReportingInstitution
import java.util.function.Function

enum class InstitutionColumnValueExtractor(
        override val parameterName: String,
        override val rowToInsertValueFunction: Function<ReportingInstitution, Any?>,
        override val queryParameterValueFunction: Function<ReportingInstitution, QueryParameterValue>) : ColumnValueExtractor<ReportingInstitution?> {
    DISPLAY_NAME(
            "display_name", Function<ReportingInstitution, Any?> { obj: ReportingInstitution -> obj.displayName }, Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValue.string(i.displayName) }),
    DUA_TYPE_ENUM(
            "dua_type_enum", Function<ReportingInstitution, Any?> { i: ReportingInstitution -> QueryParameterValues.enumToString<DuaType>(i.duaTypeEnum) }, Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValues.enumToQpv<DuaType>(i.duaTypeEnum) }),
    INSTITUTION_ID(
            "institution_id", Function<ReportingInstitution, Any?> { obj: ReportingInstitution -> obj.institutionId }, Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValue.int64(i.institutionId) }),
    ORGANIZATION_TYPE_ENUM(
            "organization_type_enum",
            Function<ReportingInstitution, Any?> { i: ReportingInstitution -> QueryParameterValues.enumToString<OrganizationType>(i.organizationTypeEnum) },
            Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValues.enumToQpv<OrganizationType>(i.organizationTypeEnum) }),
    ORGANIZATION_TYPE_OTHER_TEXT(
            "organization_type_other_text", Function<ReportingInstitution, Any?> { obj: ReportingInstitution -> obj.organizationTypeOtherText },
            Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValue.string(i.organizationTypeOtherText) }),
    SHORT_NAME("short_name", Function<ReportingInstitution, Any?> { obj: ReportingInstitution -> obj.shortName }, Function<ReportingInstitution, QueryParameterValue> { i: ReportingInstitution -> QueryParameterValue.string(i.shortName) });

    companion object {
        // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
        // as enums can't be abstract or extend abstract classes.
        const val TABLE_NAME = "institution"
    }
}
