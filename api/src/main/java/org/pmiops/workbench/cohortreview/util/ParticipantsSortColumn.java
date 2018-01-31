package org.pmiops.workbench.cohortreview.util;


public enum ParticipantsSortColumn {
    PARTICIPANT_ID("participantId", "participant_id", DatabaseType.LONG),
    STATUS("status", "status", DatabaseType.LONG),
    GENDER("gender", "gender.concept_name", DatabaseType.STRING),
    BIRTH_DATE("birthDate", "birth_date", DatabaseType.DATE),
    RACE("race", "race.concept_name", DatabaseType.STRING),
    ETHNICITY("ethnicity", "ethnicity.concept_name", DatabaseType.STRING);

    private final String name;
    private final String dbName;
    private final DatabaseType databaseType;

    private ParticipantsSortColumn(String name, String dbName, DatabaseType databaseType) {
        this.name = name;
        this.dbName = dbName;
        this.databaseType = databaseType;
    }

    public String getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public static ParticipantsSortColumn fromName(String name) {
        for (ParticipantsSortColumn sortColumn : ParticipantsSortColumn.values()) {
            if (String.valueOf(sortColumn.name).equals(name)) {
                return sortColumn;
            }
        }
        return null;
    }

    public static ParticipantsSortColumn fromDbName(String dbName) {
        for (ParticipantsSortColumn sortColumn : ParticipantsSortColumn.values()) {
            if (String.valueOf(sortColumn.dbName).equals(dbName)) {
                return sortColumn;
            }
        }
        return null;
    }

    public static boolean isDatabaseTypeLong(String property) {
        return fromName(property).getDatabaseType().equals(DatabaseType.LONG);
    }

    public static boolean isDatabaseTypeDate(String property) {
        return fromName(property).getDatabaseType().equals(DatabaseType.DATE);
    }

    public enum DatabaseType {
        STRING, LONG, DATE;
    }
}
