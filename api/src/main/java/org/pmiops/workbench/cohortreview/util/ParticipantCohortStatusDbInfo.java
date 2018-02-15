package org.pmiops.workbench.cohortreview.util;


import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public enum ParticipantCohortStatusDbInfo {
    PARTICIPANT_ID(ParticipantCohortStatusColumns.PARTICIPANTID, "participant_id", ParticipantCohortStatusDbInfo::buildLongSql),
    STATUS(ParticipantCohortStatusColumns.STATUS, "status", ParticipantCohortStatusDbInfo::buildLongSql),
    GENDER(ParticipantCohortStatusColumns.GENDER, "gender.concept_name", ParticipantCohortStatusDbInfo::buildStringSql),
    BIRTH_DATE(ParticipantCohortStatusColumns.BIRTHDATE, "birth_date", ParticipantCohortStatusDbInfo::buildDateSql),
    RACE(ParticipantCohortStatusColumns.RACE, "race.concept_name", ParticipantCohortStatusDbInfo::buildStringSql),
    ETHNICITY(ParticipantCohortStatusColumns.ETHNICITY, "ethnicity.concept_name", ParticipantCohortStatusDbInfo::buildStringSql);

    private final ParticipantCohortStatusColumns name;
    private final String dbName;
    private final BiFunction<Filter, MapSqlParameterSource, String> function;

    private ParticipantCohortStatusDbInfo(ParticipantCohortStatusColumns name, String dbName, BiFunction<Filter, MapSqlParameterSource, String> function) {
        this.name = name;
        this.dbName = dbName;
        this.function = function;
    }

    public ParticipantCohortStatusColumns getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public BiFunction<Filter, MapSqlParameterSource, String> getFunction() { return function; }

    public static ParticipantCohortStatusDbInfo fromName(ParticipantCohortStatusColumns name) {
        for (ParticipantCohortStatusDbInfo column : values()) {
            if (column.name.equals(name)) {
                return column;
            }
        }
        return null;
    }

    private static String buildLongSql(Filter filter, MapSqlParameterSource parameters) {
        validateFilterSize(filter);
        try{
            if (filter.getOperator().equals(Operator.IN)) {
                parameters.addValue(filter.getProperty().toString(), filter.getValues()
                        .stream()
                        .map(v -> filter.getProperty().equals(STATUS.getName()) ? new Long(CohortStatus.valueOf(v).ordinal()) : new Long(v))
                        .collect(Collectors.toList()));
            } else  {
                String wildcard = (filter.getOperator().equals(Operator.LIKE) ? "%" : "");
                parameters.addValue(filter.getProperty().toString(),
                        filter.getProperty().equals(STATUS.getName()) ?
                                new Long(CohortStatus.valueOf(filter.getValues().get(0)).ordinal()) + wildcard :
                                new Long(filter.getValues().get(0)) + wildcard);
            }
        } catch (Exception ex) {
            throw new BadRequestException("Problems parsing " + filter.getProperty().toString() + ": " + ex.getMessage());
        }
        return buildSqlString(filter);
    }

    private static String buildStringSql(Filter filter, MapSqlParameterSource parameters) {
        validateFilterSize(filter);
        try {
            if (filter.getOperator().equals(Operator.IN)) {
                parameters.addValue(filter.getProperty().toString(), filter.getValues()
                        .stream()
                        .map(v -> new String(v))
                        .collect(Collectors.toList()));
            } else {
                String wildcard = (filter.getOperator().equals(Operator.LIKE) ? "%" : "");
                parameters.addValue(filter.getProperty().toString(), filter.getValues().get(0) + wildcard);
            }
        } catch (Exception ex) {
            throw new BadRequestException("Problems parsing " + filter.getProperty().toString() + ": " + ex.getMessage());
        }
        return buildSqlString(filter);
    }

    private static String buildDateSql(Filter filter, MapSqlParameterSource parameters) {
        validateFilterSize(filter);
        if (filter.getOperator().equals(Operator.IN)) {
            parameters.addValue(filter.getProperty().toString(), filter.getValues()
                    .stream()
                    .map(v -> parseDate(filter.getProperty().toString(), v))
                    .collect(Collectors.toList()));
        } else {
            String wildcard = (filter.getOperator().equals(Operator.LIKE) ? "%" : "");
            parameters.addValue(filter.getProperty().toString(),
                    parseDate(filter.getProperty().toString(), filter.getValues().get(0)) + wildcard);
        }
        return buildSqlString(filter);
    }

    private static String buildSqlString(Filter filter) {
        if (filter.getOperator().equals(Operator.IN)) {
            return fromName(filter.getProperty()).getDbName() +
                    " " + filter.getOperator().toString() + " (:" + filter.getProperty().toString() + ")\n";
        }
        return fromName(filter.getProperty()).getDbName() +
                " " + filter.getOperator().toString() + " :" + filter.getProperty().toString() + "\n";
    }

    private static void validateFilterSize(Filter filter) {
        if (filter.getValues().isEmpty()) {
            throw new BadRequestException("Invalid request: property: " + filter.getProperty() + " values: is empty.");
        }
        if (!filter.getOperator().equals(Operator.IN) && filter.getValues().size() > 1) {
            throw new BadRequestException("Invalid request: property: " + filter.getProperty()
                    + " using operartor: " + filter.getOperator().name() + " must have a single value.");
        }
    }

    private static Date parseDate(String property, String dateString) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return new Date(df.parse(dateString).getTime());
        } catch (Exception ex) {
            throw new BadRequestException("Problems parsing " + property + ": " + ex.getMessage());
        }
    }
}
