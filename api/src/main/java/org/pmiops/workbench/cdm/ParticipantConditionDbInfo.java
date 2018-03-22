package org.pmiops.workbench.cdm;

import org.pmiops.workbench.model.ParticipantConditionsColumns;

public enum ParticipantConditionDbInfo {

    ITEMDATE(ParticipantConditionsColumns.ITEMDATE, "item_date"),
    STANDARDVOCABULARY(ParticipantConditionsColumns.STANDARDVOCABULARY, "standard_vocabulary"),
    STANDARDNAME(ParticipantConditionsColumns.STANDARDNAME, "standard_name"),
    SOURCEVALUE(ParticipantConditionsColumns.SOURCEVALUE, "source_value"),
    SOURCEVOCABULARY(ParticipantConditionsColumns.SOURCEVOCABULARY, "source_vocabulary"),
    SOURCENAME(ParticipantConditionsColumns.SOURCENAME, "source_name");

    private final ParticipantConditionsColumns name;
    private final String dbName;

    private ParticipantConditionDbInfo(ParticipantConditionsColumns name, String dbName) {
        this.name = name;
        this.dbName = dbName;
    }

    public ParticipantConditionsColumns getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public static ParticipantConditionDbInfo fromName(ParticipantConditionsColumns name) {
        for (ParticipantConditionDbInfo column : values()) {
            if (column.name.equals(name)) {
                return column;
            }
        }
        return null;
    }
}
