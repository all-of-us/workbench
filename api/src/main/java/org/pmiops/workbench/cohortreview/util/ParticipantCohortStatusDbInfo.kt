package org.pmiops.workbench.cohortreview.util

import java.sql.Date
import java.text.SimpleDateFormat
import java.util.function.Function
import java.util.stream.Collectors
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.Filter
import org.pmiops.workbench.model.FilterColumns
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.utils.OperatorUtils
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

enum class ParticipantCohortStatusDbInfo private constructor(
        val name: FilterColumns, val dbName: String, val function: Function<String, Any>) {
    PARTICIPANT_ID(FilterColumns.PARTICIPANTID, "participant_id", Function<String, Any> { java.lang.Long(it) }),
    STATUS(FilterColumns.STATUS, "status", Function<String, Any> { parseShort(it) }),
    GENDER(FilterColumns.GENDER, "gender_concept_id", Function<String, Any> { java.lang.Long(it) }),
    BIRTH_DATE(FilterColumns.BIRTHDATE, "birth_date", Function<String, Any> { parseDate(it) }),
    RACE(FilterColumns.RACE, "race_concept_id", Function<String, Any> { java.lang.Long(it) }),
    ETHNICITY(FilterColumns.ETHNICITY, "ethnicity_concept_id", Function<String, Any> { java.lang.Long(it) }),
    DECEASED(FilterColumns.DECEASED, "deceased", Function<String, Any> { java.lang.Long(it) });


    companion object {
        private val DEFAULT_SQL = "%s %s :%s\n"
        private val IN_SQL = "%s %s (:%s)\n"
        private val BETWEEN_SQL = "%s %s :%s and :%s\n"
        private val LIKE_SQL = DEFAULT_SQL

        fun applyFunction(name: String, value: String): Any? {
            for (column in values()) {
                if (column.name.equals(FilterColumns.fromValue(name))) {
                    return column.function.apply(value)
                }
            }
            return null
        }

        fun getDbName(name: String): String? {
            for (column in values()) {
                if (column.name.equals(FilterColumns.fromValue(name))) {
                    return column.dbName
                }
            }
            return null
        }

        fun buildSql(filter: Filter, parameters: MapSqlParameterSource): String {
            val filterSql: String
            val name = filter.getProperty().name()
            validateFilterSize(filter)
            try {
                when (filter.getOperator()) {
                    IN -> {
                        filterSql = String.format(
                                IN_SQL,
                                getDbName(filter.getProperty().name()),
                                OperatorUtils.getSqlOperator(filter.getOperator()),
                                filter.getProperty().toString())
                        parameters.addValue(
                                filter.getProperty().toString(),
                                filter.getValues().stream()
                                        .map({ v -> applyFunction(name, v) })
                                        .collect(Collectors.toList<T>()))
                    }
                    LIKE -> {
                        filterSql = String.format(
                                LIKE_SQL,
                                getDbName(filter.getProperty().name()),
                                OperatorUtils.getSqlOperator(filter.getOperator()),
                                filter.getProperty().toString())
                        parameters.addValue(
                                filter.getProperty().toString(),
                                applyFunction(name, filter.getValues().get(0))!!.toString() + "%")
                    }
                    BETWEEN -> {
                        filterSql = String.format(
                                BETWEEN_SQL,
                                getDbName(filter.getProperty().name()),
                                OperatorUtils.getSqlOperator(filter.getOperator()),
                                filter.getProperty().toString() + "1",
                                filter.getProperty().toString() + "2")
                        parameters.addValue(
                                filter.getProperty().toString() + "1",
                                applyFunction(name, filter.getValues().get(0)))
                        parameters.addValue(
                                filter.getProperty().toString() + "2",
                                applyFunction(name, filter.getValues().get(1)))
                    }
                    else -> {
                        filterSql = String.format(
                                DEFAULT_SQL,
                                getDbName(filter.getProperty().name()),
                                OperatorUtils.getSqlOperator(filter.getOperator()),
                                filter.getProperty().toString())
                        parameters.addValue(
                                filter.getProperty().toString(), applyFunction(name, filter.getValues().get(0)))
                    }
                }
            } catch (ex: Exception) {
                throw BadRequestException(
                        String.format(
                                "Bad Request: Problems parsing %s: " + ex.message,
                                filter.getProperty().toString()))
            }

            return filterSql
        }

        private fun validateFilterSize(filter: Filter) {
            if (filter.getValues().isEmpty()) {
                throw BadRequestException(
                        String.format("Bad Request: property %s is empty.", filter.getProperty().name()))
            }
            if (filter.getOperator().equals(Operator.BETWEEN) && filter.getValues().size() !== 2) {
                throw BadRequestException(
                        String.format(
                                "Bad Request: property %s using operator %s must have 2 values.",
                                filter.getProperty().name(), filter.getOperator().name()))
            }
            if (!filter.getOperator().equals(Operator.IN)
                    && !filter.getOperator().equals(Operator.BETWEEN)
                    && filter.getValues().size() > 1) {
                throw BadRequestException(
                        String.format(
                                "Bad Request: property %s using operator %s must have a single value.",
                                filter.getProperty().name(), filter.getOperator().name()))
            }
        }

        private fun parseShort(v: String): Any? {
            return StorageEnums.cohortStatusToStorage(CohortStatus.valueOf(v))
        }

        private fun parseDate(v: String): Any {
            try {
                return Date(SimpleDateFormat("yyyy-MM-dd").parse(v).time)
            } catch (ex: Exception) {
                throw BadRequestException(String.format(ex.message))
            }

        }
    }
}
