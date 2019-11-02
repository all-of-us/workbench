package org.pmiops.workbench.cohortreview

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.TimeZone
import java.util.stream.Collectors
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.model.AnnotationQuery
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.CohortStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
// TODO(RW-499): use a library to construct the SQL below, rather than concatenating strings
class AnnotationQueryBuilder @Autowired
internal constructor(
        private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
        private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao) {

    class AnnotationResults(val results: Iterable<Map<String, Any>>, val columns: List<String>)

    private fun getAnnotationDefinitions(
            cohortReview: CohortReview): Map<String, CohortAnnotationDefinition> {
        return Maps.uniqueIndex(
                cohortAnnotationDefinitionDao.findByCohortId(cohortReview.cohortId),
                Function<CohortAnnotationDefinition, String> { it.getColumnName() })
    }

    private fun getSelectAndFromSql(
            cohortReview: CohortReview,
            columns: List<String>,
            annotationDefinitions: Map<String, CohortAnnotationDefinition>?,
            columnAliasMap: MutableMap<String, String>,
            parameters: ImmutableMap.Builder<String, Any>): String {
        var annotationDefinitions = annotationDefinitions
        val selectBuilder = StringBuilder("SELECT ")
        val fromBuilder = StringBuilder("\nFROM participant_cohort_status pcs")
        var annotationCount = 0
        var firstColumn = true
        for (column in columns) {
            if (firstColumn) {
                firstColumn = false
            } else {
                selectBuilder.append(", ")
            }
            if (column == PERSON_ID_COLUMN) {
                selectBuilder.append("pcs.participant_id person_id")
            } else if (column == REVIEW_STATUS_COLUMN) {
                selectBuilder.append("pcs.status review_status")
            } else {
                if (annotationDefinitions == null) {
                    annotationDefinitions = getAnnotationDefinitions(cohortReview)
                }
                val definition = annotationDefinitions[column]
                        ?: throw BadRequestException("Invalid annotation name: $column")
                annotationCount++
                fromBuilder.append(
                        String.format(
                                ANNOTATION_JOIN_SQL,
                                annotationCount,
                                annotationCount,
                                annotationCount,
                                annotationCount,
                                annotationCount,
                                cohortReview.cohortReviewId))
                parameters.put("def_$annotationCount", definition.cohortAnnotationDefinitionId)
                val sourceColumn: String
                if (definition.annotationTypeEnum.equals(AnnotationType.ENUM)) {
                    sourceColumn = String.format("ae%d.name", annotationCount)
                    fromBuilder.append(
                            String.format(
                                    ANNOTATION_VALUE_JOIN_SQL, annotationCount, annotationCount, annotationCount))
                } else {

                    val columnName = ANNOTATION_COLUMN_MAP[definition.annotationTypeEnum]
                            ?: throw ServerErrorException(
                                    "Invalid annotation type: " + definition.annotationTypeEnum)
                    sourceColumn = String.format("a%d.%s", annotationCount, columnName)
                }
                val columnAliasName = String.format("av%d", annotationCount)
                selectBuilder.append(String.format("%s %s", sourceColumn, columnAliasName))
                columnAliasMap[column] = columnAliasName
            }
        }
        return selectBuilder.toString() + fromBuilder.toString()
    }

    private fun getWhereSql(
            cohortReview: CohortReview,
            statusFilter: List<CohortStatus>,
            parameters: ImmutableMap.Builder<String, Any>): String {
        val whereBuilder = StringBuilder(" WHERE pcs.cohort_review_id = :cohort_review_id")
        parameters.put("cohort_review_id", cohortReview.cohortReviewId)
        whereBuilder.append(" AND pcs.status IN (:statuses)")
        parameters.put(
                "statuses", statusFilter.stream().map(Function<CohortStatus, Any> { CohortStatus.ordinal() }).collect(Collectors.toList<Any>()))
        return whereBuilder.toString()
    }

    private fun getOrderBySql(orderBy: List<String>, columnAliasMap: Map<String, String>): String {
        val orderByBuilder = StringBuilder("\nORDER BY ")
        var firstOrderByColumn = true
        for (orderByColumn in orderBy) {
            if (firstOrderByColumn) {
                firstOrderByColumn = false
            } else {
                orderByBuilder.append(", ")
            }
            var descending = false
            if (orderByColumn.startsWith(DESCENDING_PREFIX)) {
                orderByColumn = orderByColumn.substring(DESCENDING_PREFIX.length, orderByColumn.length - 1)
                descending = true
            }

            if (orderByColumn == PERSON_ID_COLUMN || orderByColumn == REVIEW_STATUS_COLUMN) {
                orderByBuilder.append(orderByColumn)
            } else {
                val columnAlias = columnAliasMap[orderByColumn]
                        ?: throw BadRequestException(
                                "Column $orderByColumn in orderBy must be present in columns")
                orderByBuilder.append(columnAlias)
            }
            if (descending) {
                orderByBuilder.append(" DESC")
            }
        }
        return orderByBuilder.toString()
    }

    private fun getLimitAndOffsetSql(
            limit: Int?, offset: Long, params: ImmutableMap.Builder<String, Any>): String {
        val endSqlBuilder: StringBuilder
        if (limit == null) {
            endSqlBuilder = StringBuilder("\n")
        } else {
            endSqlBuilder = StringBuilder("\nLIMIT :limit")
            params.put("limit", limit)
        }
        if (offset != 0L) {
            // TODO: consider pagination based on values rather than offsets
            endSqlBuilder.append(" OFFSET :offset")
            params.put("offset", offset)
        }
        return endSqlBuilder.toString()
    }

    private fun getSql(
            cohortReview: CohortReview,
            statusFilter: List<CohortStatus>,
            annotationQuery: AnnotationQuery,
            limit: Int?,
            offset: Long,
            annotationDefinitions: Map<String, CohortAnnotationDefinition>?,
            parameters: ImmutableMap.Builder<String, Any>): String {
        val columnAliasMap = Maps.newHashMap<String, String>()
        val selectAndFromSql = getSelectAndFromSql(
                cohortReview,
                annotationQuery.getColumns(),
                annotationDefinitions,
                columnAliasMap,
                parameters)
        val whereSql = getWhereSql(cohortReview, statusFilter, parameters)
        val orderBySql = getOrderBySql(annotationQuery.getOrderBy(), columnAliasMap)
        val limitAndOffsetSql = getLimitAndOffsetSql(limit, offset, parameters)
        val sqlBuilder = StringBuilder(selectAndFromSql)
        sqlBuilder.append(whereSql)
        sqlBuilder.append(orderBySql)
        sqlBuilder.append(limitAndOffsetSql)
        return sqlBuilder.toString()
    }

    fun materializeAnnotationQuery(
            cohortReview: CohortReview,
            statusFilter: List<CohortStatus>?,
            annotationQuery: AnnotationQuery,
            limit: Int?,
            offset: Long): AnnotationResults {
        if (statusFilter == null || statusFilter.isEmpty()) {
            throw BadRequestException("statusFilter cannot be empty")
        }
        var annotationDefinitions: Map<String, CohortAnnotationDefinition>? = null
        var columns = annotationQuery.getColumns()
        if (columns == null || columns!!.isEmpty()) {
            // By default get person_id, review_status, and all the annotation definitions.
            columns = ArrayList<String>()
            columns!!.add(PERSON_ID_COLUMN)
            columns!!.add(REVIEW_STATUS_COLUMN)
            annotationDefinitions = getAnnotationDefinitions(cohortReview)
            columns!!.addAll(
                    annotationDefinitions.values.stream()
                            .map<String>(Function<CohortAnnotationDefinition, String> { it.getColumnName() })
                            .collect<List<String>, Any>(Collectors.toList()))
            annotationQuery.setColumns(columns)
        }
        val orderBy = annotationQuery.getOrderBy()
        if (orderBy == null || orderBy!!.isEmpty()) {
            annotationQuery.setOrderBy(ImmutableList.of<E>(PERSON_ID_COLUMN))
        }
        val parameters = ImmutableMap.builder<String, Any>()
        val sql = getSql(
                cohortReview,
                statusFilter,
                annotationQuery,
                limit,
                offset,
                annotationDefinitions,
                parameters)
        val results = namedParameterJdbcTemplate.query(
                sql,
                parameters.build()
        ) { rs, rowNum ->
            val result = ImmutableMap.builder<String, Any>()
            val columns = annotationQuery.getColumns()
            for (i in columns.indices) {
                val obj = rs.getObject(i + 1)
                if (obj != null) {
                    val column = columns.get(i)
                    if (column == REVIEW_STATUS_COLUMN) {
                        result.put(
                                column,
                                StorageEnums.cohortStatusFromStorage((obj as Number).toShort()).name())
                    } else if (obj is java.sql.Date) {
                        result.put(column, DATE_FORMAT.format(obj as java.sql.Date))
                    } else {
                        result.put(column, obj!!)
                    }
                }
            }
            result.build()
        }
        return AnnotationResults(results, annotationQuery.getColumns())
    }

    companion object {

        val PERSON_ID_COLUMN = "person_id"
        val REVIEW_STATUS_COLUMN = "review_status"

        // These column names are reserved and can't be used for annotation definition column names.
        val RESERVED_COLUMNS = ImmutableSet.of(PERSON_ID_COLUMN, REVIEW_STATUS_COLUMN)
        private val ANNOTATION_COLUMN_MAP = ImmutableMap.of<AnnotationType, String>(
                AnnotationType.BOOLEAN, "annotation_value_boolean",
                AnnotationType.DATE, "annotation_value_date",
                AnnotationType.INTEGER, "annotation_value_integer",
                AnnotationType.STRING, "annotation_value_string")

        val DESCENDING_PREFIX = "DESCENDING("

        private val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
        private val DATE_FORMAT = SimpleDateFormat(DATE_FORMAT_PATTERN)

        init {
            DATE_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
        }

        private val ANNOTATION_JOIN_SQL = (
                " LEFT OUTER JOIN participant_cohort_annotations a%d "
                        + "ON a%d.participant_id = pcs.participant_id "
                        + "AND a%d.cohort_annotation_definition_id = :def_%d "
                        + "AND a%d.cohort_review_id = %d")

        private val ANNOTATION_VALUE_JOIN_SQL = " LEFT OUTER JOIN cohort_annotation_enum_value ae%d " + "ON ae%d.cohort_annotation_enum_value_id = a%d.cohort_annotation_enum_value_id"
    }
}
