package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParticipantCohortStatusDaoImpl implements ParticipantCohortStatusDaoCustom {

    private static final String SQL_TEMPLATE = "insert into participant_cohort_status(" +
            "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, " +
            "status, cohort_review_id, participant_id) " +
            "values";
    private static final String NEXT_INSERT = " (%s, %d, %d, %d, %d, %d, %d)";
    private static final int BATCH_SIZE = 50;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Logger log = Logger.getLogger(ParticipantCohortStatusDaoImpl.class.getName());

    @Override
    public void saveParticipantCohortStatusesCustom(List<ParticipantCohortStatus> participantCohortStatuses) {
        Statement statement = null;
        Connection connection = null;
        int index = 0;
        String sqlStatement = SQL_TEMPLATE;

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
                sqlStatement = sqlStatement.equals(SQL_TEMPLATE)
                        ? sqlStatement + nextSql : sqlStatement + ", " + nextSql;

                if(++index % BATCH_SIZE == 0) {
                    statement.execute(sqlStatement);
                    sqlStatement = SQL_TEMPLATE;
                }
            }

            if (!sqlStatement.equals(SQL_TEMPLATE)) {
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
