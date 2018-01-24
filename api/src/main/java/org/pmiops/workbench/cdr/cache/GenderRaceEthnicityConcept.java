package org.pmiops.workbench.cdr.cache;

import org.pmiops.workbench.cohortreview.SortOrder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenderRaceEthnicityConcept {

    private Map<String, Map<Long, String>> concepts;

    public GenderRaceEthnicityConcept(Map<String, Map<Long, String>> concepts) {
        this.concepts = concepts;
    }

    public Map<String, Map<Long, String>> getConcepts() {
        return this.concepts;
    }

    public List<Long> getConceptIdsByTypeAndOrder(GenderRaceEthnicityType type, SortOrder order) {
        Map<Long, String> typeMap = concepts.get(type.name());
        if (order.equals(SortOrder.asc)) {
            return typeMap.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        return typeMap.entrySet().stream()
                .sorted(Map.Entry.<Long, String>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
