package org.pmiops.workbench.cohortbuilder.querybuilder;

import java.util.Arrays;
import java.util.List;

public enum FactoryKey {
    CRITERIA("criteria-type",  Arrays.asList("ICD9-TREE", "ICD10-TREE", "CPT-TREE")),
    CODES("codes-type", Arrays.asList("ICD9", "ICD10", "CPT")),
    /** TODO: this is temporary and will be removed when we figure out the conceptId mappings **/
    GROUP_CODES("group-codes-type", Arrays.asList("GROUP_CODES")),
    DEMO("demo-type", Arrays.asList("DEMO"));

    private String name;
    private List<String> types;

    private FactoryKey(String name, List<String> types) {
        this.name = name;
        this.types = types;
    }

    public String getName() {
        return this.name;
    }

    public static String getKey(String type) {
        for (FactoryKey key: values()) {
            if (key.types.contains(type)) {
                return key.getName();
            }
        }
        return CRITERIA.getName();
    }
}
