package org.pmiops.workbench.cdr.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.cohortreview.util.SortOrder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class GenderRaceEthnicityConceptTest {

    GenderRaceEthnicityConcept greConcept;

    @Before
    public void setUp() {
        Map<Long, String> ethnicities = new HashMap<>();
        ethnicities.put(38003564L, "Not Hispanic or Latino");
        ethnicities.put(38003563L, "Hispanic or Latino");

        Map<Long, String> races = new HashMap<>();
        races.put(8515L, "Asian");
        races.put(38003587L, "Malaysian");
        races.put(38003604L, "Dominica Islander");
        races.put(38003583L, "Indonesian");
        races.put(38003616L, "Arab");
        races.put(38003614L, "European");

        Map<Long, String> genders = new HashMap<>();
        genders.put(8507L, "MALE");
        genders.put(8532L, "FEMALE");
        genders.put(8551L, "UNKNOWN");

        Map<String, Map<Long, String>> concepts = new HashMap<>();
        concepts.put(GenderRaceEthnicityType.ETHNICITY.name(), ethnicities);
        concepts.put(GenderRaceEthnicityType.RACE.name(), races);
        concepts.put(GenderRaceEthnicityType.GENDER.name(), genders);

        greConcept = new GenderRaceEthnicityConcept(concepts);
    }

    @Test
    public void getConceptIdsByTypeAndOrder() throws Exception {
        List<Long> expectedEthnicitiesAsc = Arrays.asList(38003563L, 38003564L);
        List<Long> actualEthnicitiesAsc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.ETHNICITY, SortOrder.asc);
        assertEquals(expectedEthnicitiesAsc, actualEthnicitiesAsc);

        List<Long> expectedEthnicitiesDesc = Arrays.asList(38003564L, 38003563L);
        List<Long> actualEthnicitiesDesc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.ETHNICITY, SortOrder.desc);
        assertEquals(expectedEthnicitiesDesc, actualEthnicitiesDesc);

        List<Long> expectedRacesAsc = Arrays.asList(38003616L, 8515L, 38003604L, 38003614L, 38003583L, 38003587L);
        List<Long> actualRacesAsc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.RACE, SortOrder.asc);
        assertEquals(expectedRacesAsc, actualRacesAsc);

        List<Long> expectedRacesDesc = Arrays.asList(38003587L, 38003583L, 38003614L, 38003604L, 8515L, 38003616L);
        List<Long> actualRacesDesc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.RACE, SortOrder.desc);
        assertEquals(expectedRacesDesc, actualRacesDesc);

        List<Long> expectedGendersAsc = Arrays.asList(8532L, 8507L, 8551L);
        List<Long> actualGendersAsc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.GENDER, SortOrder.asc);
        assertEquals(expectedGendersAsc, actualGendersAsc);

        List<Long> expectedGendersDesc = Arrays.asList(8551L, 8507L, 8532L);
        List<Long> actualGendersDesc = greConcept.getConceptIdsByTypeAndOrder(GenderRaceEthnicityType.GENDER, SortOrder.desc);
        assertEquals(expectedGendersDesc, actualGendersDesc);
    }

}
