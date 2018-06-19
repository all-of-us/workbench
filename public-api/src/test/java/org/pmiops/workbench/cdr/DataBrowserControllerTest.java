package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.pmiops.workbench.cdr.model.ConceptRelationshipId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class DataBrowserControllerTest {

    @Autowired
    ConceptDao conceptDao;

    @Autowired
    ConceptRelationshipDao conceptRelationshipDao;

    @PersistenceContext
    private EntityManager entityManager;

    Concept concept1 = makeConcept(123L, "concept a", "S", "conceptA", "classId", "V1", "Condition", 123L, 0.2F, 0L);
    Concept concept2 = makeConcept(456L, "concept b", null, "conceptB", "classId", "V2", "Condition", 456L, 0.2F, 0L);
    Concept concept3 = makeConcept(789L, "concept c", null, "conceptC", "classId", "V3", "Condition", 567L, 0.2F, 0L);
    Concept concept4 = makeConcept(1234L, "concept D test concept 1", null, "conceptD", "classId", "V4", "Measurement", 567L, 0.2F, 0L);
    Concept concept5 = makeConcept(5678L, "concept E test concept 1", "S", "conceptE", "classId", "V5", "Condition", 1234L, 0.2F, 0L);
    Concept concept6 = makeConcept(7890L, "concept F test concept 2", null, "conceptF", "classId", "V6", "Condition", 1234L, 0.2f, 0L);


    @Before
    public void setUp() {

        conceptDao.save(makeConcept(123L, "concept a", "S", "conceptA", "classId", "V1", "Condition", 123L, 0.2F, 0L));
        conceptDao.save(makeConcept(456L, "concept b", null, "conceptB", "classId", "V2", "Condition", 456L, 0.2F, 0L));
        conceptDao.save(makeConcept(789L, "concept c", null, "conceptC", "classId", "V3", "Condition", 567L, 0.2F, 0L));
        conceptDao.save(makeConcept(1234L, "concept D test concept 1", null, "conceptD", "classId", "V4", "Measurement", 567L, 0.2F, 0L));
        conceptDao.save(makeConcept(5678L, "concept E test concept 1", "S", "conceptE", "classId", "V5", "Condition", 1234L, 0.2F, 0L));
        conceptDao.save(makeConcept(7890L, "concept F test concept 2", null, "conceptF", "classId", "V6", "Condition", 1234L, 0.2f, 0L));

        conceptRelationshipDao.save(makeConceptRelationship(1234L, 5678L, "Maps to"));
        conceptRelationshipDao.save(makeConceptRelationship(456L, 5678L, "Maps to"));

    }

    @Test
    public void findAllConceptRelationships() throws Exception {
        /* Todo write more tests */
        final List<ConceptRelationship> list = conceptRelationshipDao.findAll();
        Assert.assertNotEquals(list,null);
    }

    @Test
    public void testConceptSearchCodeMatch() throws Exception{
        final List<Concept> list = conceptDao.findStandardConcepts(1234L);
        Assert.assertEquals(list.get(0),concept5);
    }


    private Concept makeConcept(long conceptId, String conceptName, String standardConcept, String conceptCode, String conceptClassId, String vocabularyId, String domainId, long count, float prevalence,
                                long sourceCountValue) {
        return new Concept()
                .conceptId(conceptId)
                .conceptName(conceptName)
                .standardConcept(standardConcept)
                .conceptCode(conceptCode)
                .conceptClassId(conceptClassId)
                .vocabularyId(vocabularyId)
                .domainId(domainId)
                .count(count)
                .prevalence(prevalence)
                .sourceCountValue(0L);
    }

    private ConceptRelationship makeConceptRelationship(long conceptId1, long conceptId2, String relationshipId) {
        ConceptRelationshipId key = new ConceptRelationshipId();
        key.setConceptId1(conceptId1);
        key.setConceptId2(conceptId2);
        key.setRelationshipId(relationshipId);

        ConceptRelationship result = new ConceptRelationship();
        result.setConceptRelationshipId(key);
        return result;
    }


    @After
    public void flush(){

        conceptDao.delete(concept1);
        conceptDao.delete(concept2);
        conceptDao.delete(concept3);
        conceptDao.delete(concept4);
        conceptDao.delete(concept5);
        conceptDao.delete(concept6);

    }


}
