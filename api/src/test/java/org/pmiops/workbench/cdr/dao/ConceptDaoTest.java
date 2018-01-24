package org.pmiops.workbench.cdr.dao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.testconfig.TestCdrJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestCdrJpaConfig.class})
@ActiveProfiles("test-cdr")
public class ConceptDaoTest {

    @Autowired
    ConceptDao conceptDao;

    private Concept ethnicityConcept;
    private Concept genderConcept;
    private Concept raceConcept;

    @Before
    public void setUp() {
        ethnicityConcept = createConcept(1L, "ethnicity", "Ethnicity");
        genderConcept = createConcept(2L, "gender", "Gender");
        raceConcept = createConcept(3L, "race", "Race");

        conceptDao.save(ethnicityConcept);
        conceptDao.save(genderConcept);
        conceptDao.save(raceConcept);
    }

    @After
    public void tearDown() {
        conceptDao.delete(ethnicityConcept);
        conceptDao.delete(genderConcept);
        conceptDao.delete(raceConcept);
    }

    @Test
    public void findGenderRaceEthnicityFromConcept() throws Exception {
        List<Concept> concepts = conceptDao.findGenderRaceEthnicityFromConcept();

        assertEquals("ethnicity", concepts.get(0).getConceptName());
        assertEquals("gender", concepts.get(1).getConceptName());
        assertEquals("race", concepts.get(2).getConceptName());
    }

    private Concept createConcept(Long conceptId, String name, String vocabularyId) {
        return new Concept().conceptId(conceptId).conceptName(name).vocabularyId(vocabularyId);
    }

}
