package org.pmiops.workbench.cohortbuilder.querybuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This enum maps specific implementations of {@link AbstractQueryBuilder} to specific
 * criteria types/searches. This enum is built specifically for use of the
 * {@link org.pmiops.workbench.cohortbuilder.QueryBuilderFactory} to get the correct
 * factory implementation.
 */
public enum FactoryKey {
    CODES,
    DEMO,
    PHECODE;

    private static final Map<String, Object> typeMap = Collections.unmodifiableMap(initializeMapping());

    public static FactoryKey getType(String type) {
        if (typeMap.containsKey(type)) {
            return ((FactoryKey)typeMap.get(type));
        }
        throw new IllegalArgumentException("Invalid type provided: " + type);
    }

    /**
     * {@link AbstractQueryBuilder} implementations will have a many to one
     * relationship with the criteria tree types. This map will only contain
     * mappings for searching for subject/person data.
     *
     * For example: ICD9, ICD10 and CPT criteria will all map to the
     * {@link CodesQueryBuilder}.
     *
     * @return
     */
    private static Map<String, Object> initializeMapping() {
        Map<String, Object> tMap = new HashMap<String, Object>();
        tMap.put("ICD9", FactoryKey.CODES);
        tMap.put("ICD10", FactoryKey.CODES);
        tMap.put("CPT", FactoryKey.CODES);
        tMap.put("DEMO", FactoryKey.DEMO);
        return tMap;
    }
}
