package org.pmiops.workbench.cdr.cache;

public enum GenderRaceEthnicityType {
    GENDER("Gender"), RACE("Race"), ETHNICITY("Ethnicity");

    private String value;

    private GenderRaceEthnicityType(String value) {
        this.value = value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    public static GenderRaceEthnicityType fromValue(String value) {
        for (GenderRaceEthnicityType b : GenderRaceEthnicityType.values()) {
            if (String.valueOf(b.value).equals(value)) {
                return b;
            }
        }
        return null;
    }
}
