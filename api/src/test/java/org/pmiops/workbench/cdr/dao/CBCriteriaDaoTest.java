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
    cbCriteriaDao.delete(sourceCriteria.getId());
    cbCriteriaDao.delete(standardCriteria.getId());
  }

  @Test
  public void findCriteriaByDomainAndCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    CBCriteria criteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(true)
      .code("001")
      .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> criteriaList =
      cbCriteriaDao.findCriteriaByDomainAndCode(domainId, Boolean.TRUE,"001", page);
    assertEquals(1, criteriaList.size());
    assertEquals(criteria, criteriaList.get(0));
    cbCriteriaDao.delete(criteria.getId());
  }

  @Test
  public void findCriteriaByDomainAndSynonyms() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    CBCriteria criteria = new CBCriteria()
      .domainId(domainId)
      .count("100")
      .standard(true)
      .synonyms("001[MEASUREMENT_rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> measurements =
      cbCriteriaDao.findCriteriaByDomainAndSynonyms(domainId, Boolean.TRUE,"001", page);
    assertEquals(1, measurements.size());
    assertEquals(criteria, measurements.get(0));
    cbCriteriaDao.delete(criteria.getId());
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
      .standard(false)
      .parentId(0);
    CBCriteria criteriaIcd10 = new CBCriteria()
      .domainId(domainId)
      .type(icd10Type)
      .hierarchy(true)
      .standard(false)
      .parentId(0);
    cbCriteriaDao.save(criteriaIcd9);
    cbCriteriaDao.save(criteriaIcd10);

    final CBCriteria actualIcd9 =
      cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd9Type, false, 0L).get(0);
    assertEquals(criteriaIcd9, actualIcd9);

    final CBCriteria actualIcd10 =
      cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd10Type, false, 0L).get(0);
    assertEquals(criteriaIcd10, actualIcd10);
    cbCriteriaDao.delete(criteriaIcd9.getId());
    cbCriteriaDao.delete(criteriaIcd10.getId());
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
    cbCriteriaDao.delete(raceParent.getId());
    cbCriteriaDao.delete(raceChild1.getId());
    cbCriteriaDao.delete(raceChild2.getId());
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndCode() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    String type = CriteriaType.LOINC.toString();
    CBCriteria labCriteria = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .standard(true)
      .count("10")
      .code("LP123")
      .synonyms("+[MEASUREMENT_rank1]");
    CBCriteria labCriteria1 = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .hierarchy(true)
      .standard(true)
      .count("101")
      .code("LP1234")
      .synonyms("+[MEASUREMENT_rank1]");
    cbCriteriaDao.save(labCriteria);
    cbCriteriaDao.save(labCriteria1);

    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> labs =
      cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(domainId, type, true,"LP123", page);
    assertEquals(2, labs.size());
    assertEquals(labCriteria1, labs.get(0));
    assertEquals(labCriteria, labs.get(1));
    cbCriteriaDao.delete(labCriteria.getId());
    cbCriteriaDao.delete(labCriteria1.getId());
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndSynonyms() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    String type = CriteriaType.SNOMED.toString();
    CBCriteria snomedCriteria = new CBCriteria()
      .domainId(domainId)
      .type(type)
      .count("10")
      .hierarchy(true)
      .standard(true)
      .synonyms("myMatch[CONDITION_rank1]");
    cbCriteriaDao.save(snomedCriteria);

    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> conditions =
      cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(domainId, type, true,"myMatch", page);
    assertEquals(1, conditions.size());
    assertEquals(snomedCriteria, conditions.get(0));
    cbCriteriaDao.delete(snomedCriteria.getId());
  }

  @Test
  public void findDrugConceptId2ByConceptId1() throws Exception {
    jdbcTemplate.execute("create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    Concept ingredient = new Concept().conceptId(1L).conceptClassId("Ingredient");
    conceptDao.save(ingredient);

    List<Integer> drugList = cbCriteriaDao.findDrugConceptId2ByConceptId1(Arrays.asList(12345L));
    assertEquals(1, drugList.size());
    assertEquals(Integer.valueOf(1), drugList.get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
    conceptDao.delete(ingredient.getConceptId());
  }

  @Test
  public void findDrugIngredientsByConceptId() throws Exception {

    CBCriteria drugCriteriaIngredient = new CBCriteria()
      .domainId(DomainType.DRUG.toString())
      .type(CriteriaType.RXNORM.toString())
      .name("ACETAMIN")
      .selectable(true)
      .path("1.2.3.4")
      .conceptId("1")
      .synonyms("[DRUG_rank1]");
    cbCriteriaDao.save(drugCriteriaIngredient);

    List<CBCriteria> drugList = cbCriteriaDao.findDrugIngredientByConceptId(Arrays.asList("1"), DomainType.DRUG.toString(), CriteriaType.RXNORM.toString());
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    cbCriteriaDao.delete(drugCriteriaIngredient.getId());
  }

  @Test
  public void findConceptId2ByConceptId1() throws Exception {
    jdbcTemplate.execute("create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertEquals(1, cbCriteriaDao.findConceptId2ByConceptId1(12345L).get(0).intValue());
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() throws Exception {
    CBCriteria criteria = new CBCriteria()
      .domainId(DomainType.CONDITION.toString())
      .type(CriteriaType.ICD10CM.toString())
      .standard(true)
      .conceptId("1")
      .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(criteria, cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(DomainType.CONDITION.toString(), true, Arrays.asList("1")).get(0));
  }

  @Test
  public void findConceptId2ByConceptId1() throws Exception {
    jdbcTemplate.execute("create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertEquals(1, cbCriteriaDao.findConceptId2ByConceptId1(12345L).get(0).intValue());
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() throws Exception {
    CBCriteria criteria = new CBCriteria()
      .domainId(DomainType.CONDITION.toString())
      .type(CriteriaType.ICD10CM.toString())
      .standard(true)
      .conceptId("1")
      .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(criteria, cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(DomainType.CONDITION.toString(), true, Arrays.asList("1")).get(0));
  }

  @Test
  public void findConceptId2ByConceptId1() throws Exception {
    jdbcTemplate.execute("create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute("insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertEquals(1, cbCriteriaDao.findConceptId2ByConceptId1(12345L).get(0).intValue());
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() throws Exception {
    CBCriteria criteria = new CBCriteria()
      .domainId(DomainType.CONDITION.toString())
      .type(CriteriaType.ICD10CM.toString())
      .standard(true)
      .conceptId("1")
      .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(criteria, cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(DomainType.CONDITION.toString(), true, Arrays.asList("1")).get(0));
  }

}
