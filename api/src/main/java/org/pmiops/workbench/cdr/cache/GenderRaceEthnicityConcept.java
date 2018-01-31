package org.pmiops.workbench.cdr.cache;

import java.util.Map;

public class GenderRaceEthnicityConcept {

    private Map<String, Map<Long, String>> concepts;

    public GenderRaceEthnicityConcept(Map<String, Map<Long, String>> concepts) {
        this.concepts = concepts;
    }

    public Map<String, Map<Long, String>> getConcepts() {
        return this.concepts;
    }
}
