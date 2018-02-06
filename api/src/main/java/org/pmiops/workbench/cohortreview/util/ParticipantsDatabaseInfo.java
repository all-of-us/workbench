package org.pmiops.workbench.cohortreview.util;


import org.pmiops.workbench.model.ParticipantCohortStatusColumns;

public enum ParticipantsDatabaseInfo {
    PARTICIPANT_ID(ParticipantCohortStatusColumns.PARTICIPANTID, "participant_id", DatabaseType.LONG),
    STATUS(ParticipantCohortStatusColumns.STATUS, "status", DatabaseType.LONG),
    GENDER(ParticipantCohortStatusColumns.GENDER, "gender.concept_name", DatabaseType.STRING),
    BIRTH_DATE(ParticipantCohortStatusColumns.BIRTHDATE, "birth_date", DatabaseType.DATE),
    RACE(ParticipantCohortStatusColumns.RACE, "race.concept_name", DatabaseType.STRING),
    ETHNICITY(ParticipantCohortStatusColumns.ETHNICITY, "ethnicity.concept_name", DatabaseType.STRING);

    private final ParticipantCohortStatusColumns name;
    private final String dbName;
    private final DatabaseType databaseType;

    private ParticipantsDatabaseInfo(ParticipantCohortStatusColumns name, String dbName, DatabaseType databaseType) {
        this.name = name;
        this.dbName = dbName;
        this.databaseType = databaseType;
    }

    public ParticipantCohortStatusColumns getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public static ParticipantsDatabaseInfo fromName(ParticipantCohortStatusColumns name) {
        for (ParticipantsDatabaseInfo sortColumn : ParticipantsDatabaseInfo.values()) {
            if (sortColumn.name.equals(name)) {
                return sortColumn;
            }
        }
        return null;
    }

    public static boolean isDatabaseTypeLong(ParticipantCohortStatusColumns property) {
        return fromName(property).getDatabaseType().equals(DatabaseType.LONG);
    }

    public static boolean isDatabaseTypeDate(ParticipantCohortStatusColumns property) {
        return fromName(property).getDatabaseType().equals(DatabaseType.DATE);
    }

    public enum DatabaseType {
        STRING, LONG, DATE;
    }
}
