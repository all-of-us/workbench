package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParticipantCohortStatusDaoImpl implements ParticipantCohortStatusDaoCustom {

    public static final String SELECT_SQL_TEMPLATE = "select cohort_review_id as cohortReviewId,\n" +
            "participant_id as participantId,\n" +
            "status,\n" +
            "gender_concept_id as genderConceptId,\n" +
            "gender.concept_name as gender,\n" +
            "birth_date as birthDate,\n" +
            "race_concept_id as raceConceptId,\n" +
            "race.concept_name as race,\n" +
            "ethnicity_concept_id as ethnicityConceptId,\n" +
            "ethnicity.concept_name as ethnicity\n" +
            "from participant_cohort_status pcs\n" +
            "left join concept gender on (gender.concept_id = pcs.gender_concept_id and gender.vocabulary_id = 'Gender')\n" +
            "left join concept race on (race.concept_id = pcs.race_concept_id and race.vocabulary_id = 'Race')\n" +
            "left join concept ethnicity on (ethnicity.concept_id = pcs.ethnicity_concept_id and ethnicity.vocabulary_id = 'Ethnicity')\n";

    private static final String WHERE_CLAUSE_TEMPLATE = "where cohort_review_id = :cohortReviewId\n";

    private static final String ORDERBY_SQL_TEMPLATE = "order by %s\n";

    private static final String LIMIT_SQL_TEMPLATE = "limit %d, %d";

    private static final String INSERT_SQL_TEMPLATE = "insert into participant_cohort_status(" +
            "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, " +
            "status, cohort_review_id, participant_id) " +
            "values";
    private static final String NEXT_INSERT = " (%s, %d, %d, %d, %d, %d, %d)";
    private static final int BATCH_SIZE = 50;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final Logger log = Logger.getLogger(ParticipantCohortStatusDaoImpl.class.getName());

    @Override
    public void saveParticipantCohortStatusesCustom(List<ParticipantCohortStatus> participantCohortStatuses) {
        Statement statement = null;
        Connection connection = null;
        int index = 0;
        String sqlStatement = INSERT_SQL_TEMPLATE;

        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            statement = connection.createStatement();
            connection.setAutoCommit(false);

            for (ParticipantCohortStatus pcs : participantCohortStatuses) {
                String birthDate = pcs.getBirthDate() == null
                        ? "NULL" : "'" + pcs.getBirthDate().toString() + "'";
                String nextSql = String.format(NEXT_INSERT,
                        birthDate,
                        pcs.getEthnicityConceptId(),
                        pcs.getGenderConceptId(),
                        pcs.getRaceConceptId(),
                        //this represents NOT_REVIEWED
                        3,
                        pcs.getParticipantKey().getCohortReviewId(),
                        pcs.getParticipantKey().getParticipantId());
                sqlStatement = sqlStatement.equals(INSERT_SQL_TEMPLATE)
                        ? sqlStatement + nextSql : sqlStatement + ", " + nextSql;

                if(++index % BATCH_SIZE == 0) {
                    statement.execute(sqlStatement);
                    sqlStatement = INSERT_SQL_TEMPLATE;
                }
            }

            if (!sqlStatement.equals(INSERT_SQL_TEMPLATE)) {
                statement.execute(sqlStatement);
            }

            connection.commit();

        } catch (SQLException ex) {
            log.log(Level.INFO, "SQLException: " + ex.getMessage());
            rollback(connection);
            throw new RuntimeException("SQLException: " + ex.getMessage(), ex);
        } finally {
            turnOnAutoCommit(connection);
            close(statement);
            close(connection);
        }
    }

    @Override
    public List<ParticipantCohortStatus> findAll(Long cohortReviewId, List<Filter> filtersList, PageRequest pageRequest) {
        ParticipantCohortStatusColumns sortColumnEnum = ParticipantCohortStatusColumns.fromValue(pageRequest.getSortColumn());
        String sortColumn = ParticipantCohortStatusDbInfo.fromName(sortColumnEnum).getDbName();

        sortColumn = (sortColumn.equals(ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName()))
                ? ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName() + " " + pageRequest.getSortOrder().name() :
                sortColumn + " " + pageRequest.getSortOrder().name() + ", " + ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName();

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("cohortReviewId", cohortReviewId);

        String sqlStatement = SELECT_SQL_TEMPLATE
                + buildFilteringSql(filtersList, parameters)
                + String.format(ORDERBY_SQL_TEMPLATE, sortColumn)
                + String.format(LIMIT_SQL_TEMPLATE, pageRequest.getPageNumber() * pageRequest.getPageSize(), pageRequest.getPageSize());

        return namedParameterJdbcTemplate.query(sqlStatement,
                parameters,
                new ParticipantCohortStatusRowMapper());
    }

    private String buildFilteringSql(List<Filter> filtersList, MapSqlParameterSource parameters) {
        List<String> sqlParts = new ArrayList<>();

        sqlParts.add(WHERE_CLAUSE_TEMPLATE);
        for (Filter filter : filtersList) {
            String sql = ParticipantCohortStatusDbInfo.fromName(filter.getProperty()).getFunction().apply(filter, parameters);
            sqlParts.add(sql);
        }

        return (!sqlParts.isEmpty()) ? String.join(" and ", sqlParts) : "";
    }

    private class ParticipantCohortStatusRowMapper implements RowMapper<ParticipantCohortStatus> {

        @Override
        public ParticipantCohortStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParticipantCohortStatusKey key = (new BeanPropertyRowMapper<>(ParticipantCohortStatusKey.class)).mapRow(rs,rowNum);
            ParticipantCohortStatus participantCohortStatus = (new BeanPropertyRowMapper<>(ParticipantCohortStatus.class)).mapRow(rs,rowNum);
            participantCohortStatus.setParticipantKey(key);
            return participantCohortStatus;
        }
    }

    private void turnOnAutoCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.log(Level.INFO, "Problem setting auto commit to true: " + e.getMessage());
                throw new RuntimeException("SQLException: " + e.getMessage(), e);
            }
        }
    }

    /**
     * This doesn't actually close the pooled connection, but is more likely to return this connection
     * back to the connection pool for reuse.
     *
     * @param connection
     */
    private void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.log(Level.INFO, "Problem closing connection: " + e.getMessage());
                throw new RuntimeException("SQLException: " + e.getMessage(), e);
            }
        }
    }

    private void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                log.log(Level.INFO, "Problem closing prepared statement: " + e.getMessage());
                throw new RuntimeException("SQLException: " + e.getMessage(), e);
            }
        }
    }

    private void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.log(Level.INFO, "Problem on rollback: " + e.getMessage());
                throw new RuntimeException("SQLException: " + e.getMessage(), e);
            }
        }
    }
}
