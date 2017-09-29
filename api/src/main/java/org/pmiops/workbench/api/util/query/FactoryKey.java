package org.pmiops.workbench.api.util.query;

import java.util.Arrays;
import java.util.List;

public enum FactoryKey {
    CRITERIA("criteria-type", Arrays.asList("icd9", "icd10", "cpt", "demo")),
    CODES("codes-type", Arrays.asList("icd9", "icd10", "cpt"));

    private String name;
    private List<String> types;

    private FactoryKey(String name, List<String> types) {
        this.name = name;
        this.types = types;
    }

    public static String getKey(String type) {
        for (FactoryKey key: values()) {
            if (key.types.contains(type)) {
                return key.name();
            }
        }
        return null;
    }
}
