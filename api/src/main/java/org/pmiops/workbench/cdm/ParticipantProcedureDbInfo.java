package org.pmiops.workbench.cdm;

import org.pmiops.workbench.model.ParticipantProceduresColumns;

public enum ParticipantProcedureDbInfo {

    ITEMDATE(ParticipantProceduresColumns.ITEMDATE, "item_date"),
    STANDARDVOCABULARY(ParticipantProceduresColumns.STANDARDVOCABULARY, "standard_vocabulary"),
    STANDARDNAME(ParticipantProceduresColumns.STANDARDNAME, "standard_name"),
    SOURCEVALUE(ParticipantProceduresColumns.SOURCEVALUE, "source_value"),
    SOURCEVOCABULARY(ParticipantProceduresColumns.SOURCEVOCABULARY, "source_vocabulary"),
    SOURCENAME(ParticipantProceduresColumns.SOURCENAME, "source_name"),
    AGE(ParticipantProceduresColumns.AGE, "age");

    private final ParticipantProceduresColumns name;
    private final String dbName;

    private ParticipantProcedureDbInfo(ParticipantProceduresColumns name, String dbName) {
        this.name = name;
        this.dbName = dbName;
    }

    public ParticipantProceduresColumns getName() {
        return this.name;
    }

    public String getDbName() {
        return this.dbName;
    }

    public static ParticipantProcedureDbInfo fromName(ParticipantProceduresColumns name) {
        for (ParticipantProcedureDbInfo column : values()) {
            if (column.name.equals(name)) {
                return column;
            }
        }
        return null;
    }
}
