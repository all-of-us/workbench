package org.pmiops.workbench.db.dao

import com.google.common.collect.ImmutableList
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.model.Filter
import org.pmiops.workbench.model.FilterColumns
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class ParticipantCohortStatusDaoImpl : ParticipantCohortStatusDaoCustom {

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Autowired
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate? = null

    override fun saveParticipantCohortStatusesCustom(
            participantCohortStatuses: List<ParticipantCohortStatus>) {
        var statement: Statement? = null
        var connection: Connection? = null
        var index = 0
        var sqlStatement = INSERT_SQL_TEMPLATE

        try {
            connection = jdbcTemplate!!.dataSource.connection
            statement = connection!!.createStatement()
            connection.autoCommit = false

            for (pcs in participantCohortStatuses) {
                val birthDate = if (pcs.birthDate == null) "NULL" else "'" + pcs.birthDate!!.toString() + "'"
                val nextSql = String.format(
                        NEXT_INSERT,
                        birthDate,
                        pcs.ethnicityConceptId,
                        pcs.genderConceptId,
                        pcs.raceConceptId,
                        // this represents NOT_REVIEWED
                        3,
                        pcs.participantKey!!.cohortReviewId,
                        pcs.participantKey!!.participantId,
                        pcs.deceased)
                sqlStatement = if (sqlStatement == INSERT_SQL_TEMPLATE)
                    sqlStatement + nextSql
                else
                    "$sqlStatement, $nextSql"

                if (++index % BATCH_SIZE == 0) {
                    statement!!.execute(sqlStatement)
                    sqlStatement = INSERT_SQL_TEMPLATE
                }
            }

            if (sqlStatement != INSERT_SQL_TEMPLATE) {
                statement!!.execute(sqlStatement)
            }

            connection.commit()

        } catch (ex: SQLException) {
            log.log(Level.INFO, "SQLException: " + ex.message)
            rollback(connection)
            throw RuntimeException("SQLException: " + ex.message, ex)
        } finally {
            turnOnAutoCommit(connection)
            close(statement)
            close(connection)
        }
    }

    override fun findAll(cohortReviewId: Long?, pageRequest: PageRequest): List<ParticipantCohortStatus> {
        val parameters = MapSqlParameterSource()
        parameters.addValue("cohortReviewId", cohortReviewId)

        val sqlStatement = (SELECT_SQL_TEMPLATE
                + buildFilteringSql(pageRequest.filters, parameters)
                + String.format(ORDERBY_SQL_TEMPLATE, getSortColumn(pageRequest))
                + String.format(
                LIMIT_SQL_TEMPLATE,
                pageRequest.page!! * pageRequest.pageSize!!,
                pageRequest.pageSize))

        return namedParameterJdbcTemplate!!.query(
                sqlStatement, parameters, ParticipantCohortStatusRowMapper())
    }

    override fun findCount(cohortReviewId: Long?, pageRequest: PageRequest): Long? {
        val parameters = MapSqlParameterSource()
        parameters.addValue("cohortReviewId", cohortReviewId)

        val sqlStatement = SELECT_COUNT_SQL_TEMPLATE + buildFilteringSql(pageRequest.filters, parameters)

        return namedParameterJdbcTemplate!!.queryForObject(sqlStatement, parameters, Long::class.java)
    }

    private fun getSortColumn(pageRequest: PageRequest): String {
        var sortColumn = pageRequest.sortColumn
        if (NON_GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
            sortColumn = ParticipantCohortStatusDbInfo.getDbName(sortColumn)

            sortColumn = if (sortColumn == ParticipantCohortStatusDbInfo.PARTICIPANT_ID.dbName)
                ParticipantCohortStatusDbInfo.PARTICIPANT_ID.dbName
                        + " "
                        + pageRequest.sortOrder!!.name()
            else
                sortColumn
                        + " "
                        + pageRequest.sortOrder!!.name()
                        + ", "
                        + ParticipantCohortStatusDbInfo.PARTICIPANT_ID.dbName
        }
        return sortColumn
    }

    private fun buildFilteringSql(filtersList: List<Filter>, parameters: MapSqlParameterSource): String {
        val sqlParts = ArrayList<String>()

        sqlParts.add(WHERE_CLAUSE_TEMPLATE)
        for (filter in filtersList) {
            val sql = ParticipantCohortStatusDbInfo.buildSql(filter, parameters)
            sqlParts.add(sql)
        }

        return if (!sqlParts.isEmpty()) sqlParts.joinToString(" and ") else ""
    }

    private inner class ParticipantCohortStatusRowMapper : RowMapper<ParticipantCohortStatus> {

        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): ParticipantCohortStatus {
            val key = BeanPropertyRowMapper(ParticipantCohortStatusKey::class.java).mapRow(rs, rowNum)
            val participantCohortStatus = BeanPropertyRowMapper(ParticipantCohortStatus::class.java).mapRow(rs, rowNum)
            participantCohortStatus.participantKey = key
            return participantCohortStatus
        }
    }

    private fun turnOnAutoCommit(connection: Connection?) {
        if (connection != null) {
            try {
                connection.autoCommit = true
            } catch (e: SQLException) {
                log.log(Level.INFO, "Problem setting auto commit to true: " + e.message)
                throw RuntimeException("SQLException: " + e.message, e)
            }

        }
    }

    /**
     * This doesn't actually close the pooled connection, but is more likely to return this connection
     * back to the connection pool for reuse.
     *
     * @param connection
     */
    private fun close(connection: Connection?) {
        if (connection != null) {
            try {
                connection.close()
            } catch (e: SQLException) {
                log.log(Level.INFO, "Problem closing connection: " + e.message)
                throw RuntimeException("SQLException: " + e.message, e)
            }

        }
    }

    private fun close(statement: Statement?) {
        if (statement != null) {
            try {
                statement.close()
            } catch (e: SQLException) {
                log.log(Level.INFO, "Problem closing prepared statement: " + e.message)
                throw RuntimeException("SQLException: " + e.message, e)
            }

        }
    }

    private fun rollback(connection: Connection?) {
        if (connection != null) {
            try {
                connection.rollback()
            } catch (e: SQLException) {
                log.log(Level.INFO, "Problem on rollback: " + e.message)
                throw RuntimeException("SQLException: " + e.message, e)
            }

        }
    }

    companion object {

        val NON_GENDER_RACE_ETHNICITY_TYPES: List<String> = ImmutableList.of(
                FilterColumns.STATUS.name(),
                FilterColumns.PARTICIPANTID.name(),
                FilterColumns.BIRTHDATE.name())

        val SELECT_SQL_TEMPLATE = (
                "select cohort_review_id as cohortReviewId,\n"
                        + "participant_id as participantId,\n"
                        + "status,\n"
                        + "gender_concept_id as genderConceptId,\n"
                        + "birth_date as birthDate,\n"
                        + "race_concept_id as raceConceptId,\n"
                        + "ethnicity_concept_id as ethnicityConceptId,\n"
                        + "deceased as deceased\n"
                        + "from participant_cohort_status pcs\n")

        val SELECT_COUNT_SQL_TEMPLATE = "select count(participant_id)\n" + "from participant_cohort_status pcs\n"

        private val WHERE_CLAUSE_TEMPLATE = "where cohort_review_id = :cohortReviewId\n"

        private val ORDERBY_SQL_TEMPLATE = "order by %s\n"

        private val LIMIT_SQL_TEMPLATE = "limit %d, %d"

        private val INSERT_SQL_TEMPLATE = (
                "insert into participant_cohort_status("
                        + "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, "
                        + "status, cohort_review_id, participant_id, deceased) "
                        + "values")
        private val NEXT_INSERT = " (%s, %d, %d, %d, %d, %d, %d, %s)"
        private val BATCH_SIZE = 50

        private val log = Logger.getLogger(ParticipantCohortStatusDaoImpl::class.java.name)
    }
}
