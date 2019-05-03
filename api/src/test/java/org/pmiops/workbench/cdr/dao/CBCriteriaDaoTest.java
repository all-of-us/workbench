package org.pmiops.workbench.cdr.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.StandardProjection;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CBCriteriaDaoTest {

  @Autowired
  private CBCriteriaDao cbCriteriaDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void findStandardProjectionByCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();

    //test that we match just one source code
    CBCriteria sourceCriteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(false)
      .code("120");
    cbCriteriaDao.save(sourceCriteria);
    List<StandardProjection> projections =
      cbCriteriaDao.findStandardProjectionByCode(domainId, "120");
    assertEquals(1, projections.size());
    assertFalse(projections.get(0).getStandard());

    //test that we match both source and standard codes
    CBCriteria standardCriteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(true)
      .code("120");
    cbCriteriaDao.save(standardCriteria);
    projections =
      cbCriteriaDao.findStandardProjectionByCode(domainId, "120");
    assertEquals(2, projections.size());
    assertTrue(projections.get(0).getStandard());
  }

  @Test
  public void findCriteriaByDomainAndCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    CBCriteria criteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(true)
      .code("001")
      .synonyms("[rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> criteriaList =
      cbCriteriaDao.findCriteriaByDomainAndCode(domainId, Boolean.TRUE,"001", page);
    assertEquals(1, criteriaList.size());
    assertEquals(criteria, criteriaList.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSynonyms() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    CBCriteria criteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(true)
      .synonyms("001");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> measurements =
      cbCriteriaDao.findCriteriaByDomainAndSynonyms(domainId, Boolean.TRUE,"001", page);
    assertEquals(1, measurements.size());
    assertEquals(criteria, measurements.get(0));
  }

  @Test
  public void findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    String icd9Type = CriteriaType.ICD9CM.toString();
    String icd10Type = CriteriaType.ICD10CM.toString();
    CBCriteria criteriaIcd9 = new CBCriteria()
      .domainId(domainId)
      .type(icd9Type)
      .hierarchy(true)
      .parentId(0);
    CBCriteria criteriaIcd10 = new CBCriteria()
      .domainId(domainId)
      .type(icd10Type)
      .hierarchy(true)
      .parentId(0);
    cbCriteriaDao.save(criteriaIcd9);
    cbCriteriaDao.save(criteriaIcd10);

    final CBCriteria actualIcd9 =
      cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd9Type, 0L).get(0);
    assertEquals(criteriaIcd9, actualIcd9);

    final CBCriteria actualIcd10 =
      cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd10Type, 0L).get(0);
    assertEquals(criteriaIcd10, actualIcd10);
  }

  @Test
  public void findCriteriaByDomainAndTypeOrderByIdAsc() throws Exception {
    String domainId = DomainType.PERSON.toString();
    String type = CriteriaType.RACE.toString();
    CBCriteria raceParent = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .name("Race")
      .parentId(0);
    cbCriteriaDao.save(raceParent);
    CBCriteria raceChild1 = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .name("Asian")
      .parentId(raceParent.getId());
    cbCriteriaDao.save(raceChild1);
    CBCriteria raceChild2 = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .name("White")
      .parentId(raceParent.getId());
    cbCriteriaDao.save(raceChild2);

    final List<CBCriteria> demoList =
      cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domainId, type);
    assertEquals(3, demoList.size());
    assertEquals(raceParent, demoList.get(0));
    assertEquals(raceChild1, demoList.get(1));
    assertEquals(raceChild2, demoList.get(2));
  }

  @Test
  public void findCriteriaByDomainAndTypeForCodeOrName() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    String type = CriteriaType.LOINC.toString();
    CBCriteria labCriteria = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .count("10")
      .synonyms("LP123");
    CBCriteria labCriteria1 = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .count("101")
      .code("LP1234");
    cbCriteriaDao.save(labCriteria);
    cbCriteriaDao.save(labCriteria1);

    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> labs =
      cbCriteriaDao.findCriteriaByDomainAndTypeForCodeOrName(domainId, type,"LP123", "LP1234", page);
    assertEquals(2, labs.size());
    assertEquals(labCriteria1, labs.get(0));
    assertEquals(labCriteria, labs.get(1));
  }

  @Test
  public void findCriteriaByDomainAndTypeForName() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    String type = CriteriaType.SNOMED.toString();
    CBCriteria snomedCriteria = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .count("10")
      .hierarchy(true)
      .synonyms("myMatch");
    cbCriteriaDao.save(snomedCriteria);

    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> conditions =
      cbCriteriaDao.findCriteriaByDomainAndTypeForName(domainId, type,"myMatch", page);
    assertEquals(1, conditions.size());
    assertEquals(snomedCriteria, conditions.get(0));
  }

  @Test
  public void findDrugBrandOrIngredientByName() throws Exception {
    String domainId = DomainType.DRUG.toString();
    String type = CriteriaType.ATC.toString();
    CBCriteria drugCriteriaIngredient = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .name("ACETAMIN")
      .selectable(true);
    CBCriteria drugCriteriaIngredient1 = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .name("MIN1")
      .code("2")
      .selectable(true);
    CBCriteria drugCriteriaBrand = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .name("BLAH")
      .selectable(true);
    cbCriteriaDao.save(drugCriteriaIngredient);
    cbCriteriaDao.save(drugCriteriaIngredient1);
    cbCriteriaDao.save(drugCriteriaBrand);

    List<CBCriteria> drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("ETAM", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("ACE", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("A", 1L);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));

    drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("A", 3L);
    assertEquals(2, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    assertEquals(drugCriteriaBrand, drugList.get(1));

    drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("BL", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaBrand, drugList.get(0));

    drugList = cbCriteriaDao.findDrugBrandOrIngredientByValue("2", null);
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient1, drugList.get(0));
  }

  @Test
  public void findDrugIngredientsByConceptId() throws Exception {
    jdbcTemplate.execute("create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    CBCriteria drugCriteriaIngredient = new CBCriteria()
      .domainId(DomainType.DRUG.toString())
      .type(CriteriaType.RXNORM.toString())
      .name("ACETAMIN")
      .selectable(true)
      .path("1.2.3.4")
      .conceptId("1");
    cbCriteriaDao.save(drugCriteriaIngredient);
    conceptDao.save(new Concept().conceptId(1L).conceptClassId("Ingredient"));

    List<CBCriteria> drugList = cbCriteriaDao.findDrugIngredientByConceptId(Arrays.asList(12345L));
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

}
