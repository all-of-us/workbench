package org.pmiops.workbench.cdr.dao;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestCdrJpaConfig.class})
@ActiveProfiles("test-cdr")
public class ConceptDaoTest {

    @Autowired
    ConceptDao conceptDao;

    @PersistenceContext
    private EntityManager em;

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
        Session session = (Session)em.getDelegate();

        conceptDao.findGenderRaceEthnicityFromConcept();

        Statistics stats = session.getSessionFactory().getStatistics();
        SecondLevelCacheStatistics slc = session
                .getSessionFactory()
                .getStatistics()
                .getSecondLevelCacheStatistics("concept");

        conceptDao.findGenderRaceEthnicityFromConcept();
        assertEquals(3L, slc.getPutCount());
        assertEquals(0L, slc.getHitCount());

        tearDown();

        conceptDao.findGenderRaceEthnicityFromConcept();

        conceptDao.findGenderRaceEthnicityFromConcept();
        assertEquals(3L, slc.getPutCount());
        assertEquals(3L, slc.getHitCount());

    }

    private Concept createConcept(Long conceptId, String name, String vocabularyId) {
        return new Concept().conceptId(conceptId).conceptName(name).vocabularyId(vocabularyId);
    }

}
