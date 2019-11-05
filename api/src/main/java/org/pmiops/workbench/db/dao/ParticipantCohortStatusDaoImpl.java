package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.cohortreview.util.ParticipantCohortStatusDbInfo;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ParticipantCohortStatusDaoImpl implements ParticipantCohortStatusDaoCustom {

  public static final List<String> NON_GENDER_RACE_ETHNICITY_TYPES =
      ImmutableList.of(
          FilterColumns.STATUS.name(),
          FilterColumns.PARTICIPANTID.name(),
          FilterColumns.BIRTHDATE.name());

  public static final String SELECT_SQL_TEMPLATE =
      "select cohort_review_id as cohortReviewId,\n"
          + "participant_id as participantId,\n"
          + "status,\n"
          + "gender_concept_id as genderConceptId,\n"
          + "birth_date as birthDate,\n"
          + "race_concept_id as raceConceptId,\n"
          + "ethnicity_concept_id as ethnicityConceptId,\n"
          + "deceased as deceased\n"
          + "from participant_cohort_status pcs\n";

  public static final String SELECT_COUNT_SQL_TEMPLATE =
      "select count(participant_id)\n" + "from participant_cohort_status pcs\n";

  private static final String WHERE_CLAUSE_TEMPLATE = "where cohort_review_id = :cohortReviewId\n";

  private static final String ORDERBY_SQL_TEMPLATE = "order by %s\n";

  private static final String LIMIT_SQL_TEMPLATE = "limit %d, %d";

  private static final String INSERT_SQL_TEMPLATE =
      "insert into participant_cohort_status("
          + "birth_date, ethnicity_concept_id, gender_concept_id, race_concept_id, "
          + "status, cohort_review_id, participant_id, deceased) "
          + "values";
  private static final String NEXT_INSERT = " (%s, %d, %d, %d, %d, %d, %d, %s)";
  private static final int BATCH_SIZE = 50;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private static final Logger log =
      Logger.getLogger(ParticipantCohortStatusDaoImpl.class.getName());

  @Override
  public void saveParticipantCohortStatusesCustom(
      List<DbParticipantCohortStatus> participantCohortStatuses) {
    Statement statement = null;
    Connection connection = null;
    int index = 0;
    String sqlStatement = INSERT_SQL_TEMPLATE;

    try {
      connection = jdbcTemplate.getDataSource().getConnection();
      statement = connection.createStatement();
      connection.setAutoCommit(false);

      for (DbParticipantCohortStatus pcs : participantCohortStatuses) {
        String birthDate =
            pcs.getBirthDate() == null ? "NULL" : "'" + pcs.getBirthDate().toString() + "'";
        String nextSql =
            String.format(
                NEXT_INSERT,
                birthDate,
                pcs.getEthnicityConceptId(),
                pcs.getGenderConceptId(),
                pcs.getRaceConceptId(),
                // this represents NOT_REVIEWED
                3,
                pcs.getParticipantKey().getCohortReviewId(),
                pcs.getParticipantKey().getParticipantId(),
                pcs.getDeceased());
        sqlStatement =
            sqlStatement.equals(INSERT_SQL_TEMPLATE)
                ? sqlStatement + nextSql
                : sqlStatement + ", " + nextSql;

        if (++index % BATCH_SIZE == 0) {
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
  public List<DbParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("cohortReviewId", cohortReviewId);

    String sqlStatement =
        SELECT_SQL_TEMPLATE
            + buildFilteringSql(pageRequest.getFilters(), parameters)
            + String.format(ORDERBY_SQL_TEMPLATE, getSortColumn(pageRequest))
            + String.format(
                LIMIT_SQL_TEMPLATE,
                pageRequest.getPage() * pageRequest.getPageSize(),
                pageRequest.getPageSize());

    return namedParameterJdbcTemplate.query(
        sqlStatement, parameters, new ParticipantCohortStatusRowMapper());
  }

  @Override
  public Long findCount(Long cohortReviewId, PageRequest pageRequest) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("cohortReviewId", cohortReviewId);

    String sqlStatement =
        SELECT_COUNT_SQL_TEMPLATE + buildFilteringSql(pageRequest.getFilters(), parameters);

    return namedParameterJdbcTemplate.queryForObject(sqlStatement, parameters, Long.class);
  }

  private String getSortColumn(PageRequest pageRequest) {
    String sortColumn = pageRequest.getSortColumn();
    if (NON_GENDER_RACE_ETHNICITY_TYPES.contains(sortColumn)) {
      sortColumn = ParticipantCohortStatusDbInfo.getDbName(sortColumn);

      sortColumn =
          (sortColumn.equals(ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName()))
              ? ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName()
                  + " "
                  + pageRequest.getSortOrder().name()
              : sortColumn
                  + " "
                  + pageRequest.getSortOrder().name()
                  + ", "
                  + ParticipantCohortStatusDbInfo.PARTICIPANT_ID.getDbName();
    }
    return sortColumn;
  }

  private String buildFilteringSql(List<Filter> filtersList, MapSqlParameterSource parameters) {
    List<String> sqlParts = new ArrayList<>();

    sqlParts.add(WHERE_CLAUSE_TEMPLATE);
    for (Filter filter : filtersList) {
      String sql = ParticipantCohortStatusDbInfo.buildSql(filter, parameters);
      sqlParts.add(sql);
    }

    return (!sqlParts.isEmpty()) ? String.join(" and ", sqlParts) : "";
  }

  private class ParticipantCohortStatusRowMapper implements RowMapper<DbParticipantCohortStatus> {

    @Override
    public DbParticipantCohortStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
      DbParticipantCohortStatusKey key =
          (new BeanPropertyRowMapper<>(DbParticipantCohortStatusKey.class)).mapRow(rs, rowNum);
      DbParticipantCohortStatus participantCohortStatus =
          (new BeanPropertyRowMapper<>(DbParticipantCohortStatus.class)).mapRow(rs, rowNum);
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
