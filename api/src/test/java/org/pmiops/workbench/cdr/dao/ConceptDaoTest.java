package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ConceptDaoTest {

  @Autowired ConceptDao conceptDao;
  private Concept ethnicityConcept;
  private Concept genderConcept;
  private Concept raceConcept;

  @Before
  public void setUp() {

    ethnicityConcept =
        conceptDao.save(
            new Concept().conceptId(1L).conceptName("ethnicity").vocabularyId("Ethnicity"));
    genderConcept =
        conceptDao.save(new Concept().conceptId(2L).conceptName("gender").vocabularyId("Gender"));
    raceConcept =
        conceptDao.save(new Concept().conceptId(3L).conceptName("race").vocabularyId("Race"));
  }

  @Test
  public void findGenderRaceEthnicityFromConcept() throws Exception {
    List<Concept> concepts = conceptDao.findGenderRaceEthnicityFromConcept();
    assertEquals(ethnicityConcept, concepts.get(0));
    assertEquals(genderConcept, concepts.get(1));
    assertEquals(raceConcept, concepts.get(2));
  }
}
