package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryParameterValue
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues
import org.pmiops.workbench.model.ReportingCohort
import java.util.function.Function

enum class CohortColumnValueExtractor(
        override val parameterName: String,
        override val rowToInsertValueFunction: Function<ReportingCohort, Any?>,
        override val queryParameterValueFunction: Function<ReportingCohort, QueryParameterValue?>) : ColumnValueExtractor<ReportingCohort?> {
    COHORT_ID("cohort_id", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.cohortId }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.int64(c.cohortId) }), CREATION_TIME(
            "creation_time",
            Function<ReportingCohort, Any?> { c: ReportingCohort -> QueryParameterValues.toInsertRowString(c.creationTime) },
            Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValues.toTimestampQpv(c.creationTime) }),
    CREATOR_ID("creator_id", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.creatorId }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.int64(c.creatorId) }), CRITERIA("criteria", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.criteria }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.string(c.criteria) }), DESCRIPTION("description", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.description }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.string(c.description) }), LAST_MODIFIED_TIME(
            "last_modified_time",
            Function<ReportingCohort, Any?> { c: ReportingCohort -> QueryParameterValues.toInsertRowString(c.lastModifiedTime) },
            Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValues.toTimestampQpv(c.lastModifiedTime) }),
    NAME("name", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.name }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.string(c.name) }), WORKSPACE_ID("workspace_id", Function<ReportingCohort, Any?> { obj: ReportingCohort -> obj.workspaceId }, Function<ReportingCohort, QueryParameterValue?> { c: ReportingCohort -> QueryParameterValue.int64(c.workspaceId) });

    companion object {
        // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
        // as enums can't be abstract or extend abstract classes.
        const val TABLE_NAME = "cohort"
    }
}
