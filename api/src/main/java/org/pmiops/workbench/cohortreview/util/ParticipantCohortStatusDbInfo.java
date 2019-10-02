package org.pmiops.workbench.cohortreview.util;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public enum ParticipantCohortStatusDbInfo {
  PARTICIPANT_ID(
      FilterColumns.PARTICIPANTID.name(),
      "participant_id",
      ParticipantCohortStatusDbInfo::buildLongSql),
  STATUS(FilterColumns.STATUS.name(), "status", ParticipantCohortStatusDbInfo::buildLongSql),
  GENDER(
      FilterColumns.GENDER.name(),
      "gender_concept_id",
      ParticipantCohortStatusDbInfo::buildLongSql),
  BIRTH_DATE(
      FilterColumns.BIRTHDATE.name(), "birth_date", ParticipantCohortStatusDbInfo::buildDateSql),
  RACE(FilterColumns.RACE.name(), "race_concept_id", ParticipantCohortStatusDbInfo::buildLongSql),
  ETHNICITY(
      FilterColumns.ETHNICITY.name(),
      "ethnicity_concept_id",
      ParticipantCohortStatusDbInfo::buildLongSql),
  DECEASED(FilterColumns.DECEASED.name(), "deceased", ParticipantCohortStatusDbInfo::buildLongSql);

  private final String name;
  private final String dbName;
  private final BiFunction<Filter, MapSqlParameterSource, String> function;

  private ParticipantCohortStatusDbInfo(
      String name, String dbName, BiFunction<Filter, MapSqlParameterSource, String> function) {
    this.name = name;
    this.dbName = dbName;
    this.function = function;
  }

  public String getName() {
    return this.name;
  }

  public String getDbName() {
    return this.dbName;
  }

  public BiFunction<Filter, MapSqlParameterSource, String> getFunction() {
    return function;
  }

  public static ParticipantCohortStatusDbInfo fromName(String name) {
    for (ParticipantCohortStatusDbInfo column : values()) {
      if (column.name.equals(name)) {
        return column;
      }
    }
    return null;
  }

  private static String buildLongSql(Filter filter, MapSqlParameterSource parameters) {
    validateFilterSize(filter);
    try {
      if (filter.getOperator().equals(Operator.IN)) {
        parameters.addValue(
            filter.getProperty().toString(),
            filter.getValues().stream()
                .map(
                    v ->
                        filter.getProperty().name().equals(STATUS.getName())
                            ? new Long(CohortStatus.valueOf(v).ordinal())
                            : new Long(v))
                .collect(Collectors.toList()));
      } else {
        String wildcard = (filter.getOperator().equals(Operator.LIKE) ? "%" : "");
        parameters.addValue(
            filter.getProperty().toString(),
            filter.getProperty().name().equals(STATUS.getName())
                ? new Long(CohortStatus.valueOf(filter.getValues().get(0)).ordinal()) + wildcard
                : new Long(filter.getValues().get(0)) + wildcard);
      }
    } catch (Exception ex) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Problems parsing %s: " + ex.getMessage(),
              filter.getProperty().toString()));
    }
    return buildSqlString(filter);
  }

  private static String buildStringSql(Filter filter, MapSqlParameterSource parameters) {
    validateFilterSize(filter);
    try {
      if (filter.getOperator().equals(Operator.IN)) {
        parameters.addValue(
            filter.getProperty().toString(),
            filter.getValues().stream().map(v -> new String(v)).collect(Collectors.toList()));
      } else {
        String wildcard = (filter.getOperator().equals(Operator.LIKE) ? "%" : "");
        parameters.addValue(filter.getProperty().toString(), filter.getValues().get(0) + wildcard);
      }
    } catch (Exception ex) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Problems parsing %s: " + ex.getMessage(),
              filter.getProperty().toString()));
    }
    return buildSqlString(filter);
  }

  private static String buildDateSql(Filter filter, MapSqlParameterSource parameters) {
    validateFilterSize(filter);
    if (filter.getOperator().equals(Operator.IN)) {
      parameters.addValue(
          filter.getProperty().toString(),
          filter.getValues().stream()
              .map(v -> parseDate(filter.getProperty().toString(), v))
              .collect(Collectors.toList()));
    } else {
      if (filter.getOperator().equals(Operator.EQUAL)) {
        parameters.addValue(
            filter.getProperty().toString(),
            parseDate(filter.getProperty().toString(), filter.getValues().get(0)));
      } else {
        parameters.addValue(filter.getProperty().toString(), filter.getValues().get(0) + "%");
      }
    }
    return buildSqlString(filter);
  }

  private static String buildSqlString(Filter filter) {
    if (filter.getProperty().equals(FilterColumns.BIRTHDATE)
        && filter.getOperator().equals(Operator.LIKE)) {
      return "cast("
          + fromName(filter.getProperty().name()).getDbName()
          + " as char)"
          + " "
          + OperatorUtils.getSqlOperator(filter.getOperator())
          + " :"
          + filter.getProperty().toString()
          + "\n";
    } else if (filter.getOperator().equals(Operator.IN)) {
      return fromName(filter.getProperty().name()).getDbName()
          + " "
          + OperatorUtils.getSqlOperator(filter.getOperator())
          + " (:"
          + filter.getProperty().toString()
          + ")\n";
    }
    return fromName(filter.getProperty().name()).getDbName()
        + " "
        + OperatorUtils.getSqlOperator(filter.getOperator())
        + " :"
        + filter.getProperty().toString()
        + "\n";
  }

  private static void validateFilterSize(Filter filter) {
    if (filter.getValues().isEmpty()) {
      throw new BadRequestException(
          String.format("Bad Request: property %s is empty.", filter.getProperty().name()));
    }
    if (!filter.getOperator().equals(Operator.IN) && filter.getValues().size() > 1) {
      throw new BadRequestException(
          String.format(
              "Bad Request: property %s using operator %s must have a single value.",
              filter.getProperty().name(), filter.getOperator().name()));
    }
  }

  private static Date parseDate(String property, String dateString) {
    try {
      DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
      return new Date(df.parse(dateString).getTime());
    } catch (Exception ex) {
      throw new BadRequestException(
          String.format("Bad Request: Problems parsing %s: " + ex.getMessage(), property));
    }
  }
}
