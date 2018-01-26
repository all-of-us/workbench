package org.pmiops.workbench.cohortreview.util;


public enum SortColumn {
    PARTICIPANT_ID("participantId", "participant_id"),
    STATUS("status", "status"),
    GENDER("gender", "gender"),
    BIRTH_DATE("birthDate", "birth_date"),
    RACE("race", "race"),
    ETHNICITY("ethnicity", "ethnicity");

    private final String name;
    private final String dbName;

    private SortColumn(String name, String dbName) {
        this.name = name;
        this.dbName = dbName;
    }

    public String getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public static SortColumn fromName(String name) {
        for (SortColumn sortColumn : SortColumn.values()) {
            if (String.valueOf(sortColumn.name).equals(name)) {
                return sortColumn;
            }
        }
        return null;
    }

    public static SortColumn fromDbName(String dbName) {
        for (SortColumn sortColumn : SortColumn.values()) {
            if (String.valueOf(sortColumn.dbName).equals(dbName)) {
                return sortColumn;
            }
        }
        return null;
    }
}
