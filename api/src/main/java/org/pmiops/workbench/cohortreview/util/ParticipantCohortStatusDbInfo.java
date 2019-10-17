package org.pmiops.workbench.cohortreview.util;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public enum ParticipantCohortStatusDbInfo {
  PARTICIPANT_ID(FilterColumns.PARTICIPANTID, "participant_id", Long::new),
  STATUS(FilterColumns.STATUS, "status", ParticipantCohortStatusDbInfo::parseShort),
  GENDER(FilterColumns.GENDER, "gender_concept_id", Long::new),
  BIRTH_DATE(FilterColumns.BIRTHDATE, "birth_date", ParticipantCohortStatusDbInfo::parseDate),
  RACE(FilterColumns.RACE, "race_concept_id", Long::new),
  ETHNICITY(FilterColumns.ETHNICITY, "ethnicity_concept_id", Long::new),
  DECEASED(FilterColumns.DECEASED, "deceased", Long::new);

  private final FilterColumns name;
  private final String dbName;
  private final Function<String, Object> function;
  private static final String DEFAULT_SQL = "%s %s :%s\n";
  private static final String IN_SQL = "%s %s (:%s)\n";
  private static final String BETWEEN_SQL = "%s %s :%s and :%s\n";
  private static final String LIKE_SQL = DEFAULT_SQL;

  ParticipantCohortStatusDbInfo(
      FilterColumns name, String dbName, Function<String, Object> function) {
    this.name = name;
    this.dbName = dbName;
    this.function = function;
  }

  public FilterColumns getName() {
    return this.name;
  }

  public String getDbName() {
    return this.dbName;
  }

  public Function<String, Object> getFunction() {
    return function;
  }

  public static Object applyFunction(String name, String value) {
    for (ParticipantCohortStatusDbInfo column : values()) {
      if (column.name.equals(FilterColumns.fromValue(name))) {
        return column.function.apply(value);
      }
    }
    return null;
  }

  public static String getDbName(String name) {
    for (ParticipantCohortStatusDbInfo column : values()) {
      if (column.name.equals(FilterColumns.fromValue(name))) {
        return column.dbName;
      }
    }
    return null;
  }

  public static String buildSql(Filter filter, MapSqlParameterSource parameters) {
    String filterSql;
    String name = filter.getProperty().name();
    validateFilterSize(filter);
    try {
      switch (filter.getOperator()) {
        case IN:
          filterSql =
              String.format(
                  IN_SQL,
                  getDbName(filter.getProperty().name()),
                  OperatorUtils.getSqlOperator(filter.getOperator()),
                  filter.getProperty().toString());
          parameters.addValue(
              filter.getProperty().toString(),
              filter.getValues().stream()
                  .map(v -> applyFunction(name, v))
                  .collect(Collectors.toList()));
          break;
        case LIKE:
          filterSql =
              String.format(
                  LIKE_SQL,
                  getDbName(filter.getProperty().name()),
                  OperatorUtils.getSqlOperator(filter.getOperator()),
                  filter.getProperty().toString());
          parameters.addValue(
              filter.getProperty().toString(),
              applyFunction(name, filter.getValues().get(0)) + "%");
          break;
        case BETWEEN:
          filterSql =
              String.format(
                  BETWEEN_SQL,
                  getDbName(filter.getProperty().name()),
                  OperatorUtils.getSqlOperator(filter.getOperator()),
                  filter.getProperty().toString() + "1",
                  filter.getProperty().toString() + "2");
          parameters.addValue(
              filter.getProperty().toString() + "1",
              applyFunction(name, filter.getValues().get(0)));
          parameters.addValue(
              filter.getProperty().toString() + "2",
              applyFunction(name, filter.getValues().get(1)));
          break;
        default:
          filterSql =
              String.format(
                  DEFAULT_SQL,
                  getDbName(filter.getProperty().name()),
                  OperatorUtils.getSqlOperator(filter.getOperator()),
                  filter.getProperty().toString());
          parameters.addValue(
              filter.getProperty().toString(), applyFunction(name, filter.getValues().get(0)));
          break;
      }
    } catch (Exception ex) {
      throw new BadRequestException(
          String.format(
              "Bad Request: Problems parsing %s: " + ex.getMessage(),
              filter.getProperty().toString()));
    }

    return filterSql;
  }

  private static void validateFilterSize(Filter filter) {
    if (filter.getValues().isEmpty()) {
      throw new BadRequestException(
          String.format("Bad Request: property %s is empty.", filter.getProperty().name()));
    }
    if (filter.getOperator().equals(Operator.BETWEEN) && filter.getValues().size() != 2) {
      throw new BadRequestException(
          String.format(
              "Bad Request: property %s using operator %s must have 2 values.",
              filter.getProperty().name(), filter.getOperator().name()));
    }
    if (!filter.getOperator().equals(Operator.IN)
        && !filter.getOperator().equals(Operator.BETWEEN)
        && filter.getValues().size() > 1) {
      throw new BadRequestException(
          String.format(
              "Bad Request: property %s using operator %s must have a single value.",
              filter.getProperty().name(), filter.getOperator().name()));
    }
  }

  private static Object parseShort(String v) {
    return StorageEnums.cohortStatusToStorage(CohortStatus.valueOf(v));
  }

  private static Object parseDate(String v) {
    try {
      return new Date(new SimpleDateFormat("yyyy-MM-dd").parse(v).getTime());
    } catch (Exception ex) {
      throw new BadRequestException(String.format(ex.getMessage()));
    }
  }
}
