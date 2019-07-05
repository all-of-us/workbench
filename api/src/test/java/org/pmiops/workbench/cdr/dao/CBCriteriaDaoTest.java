package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CBCriteriaDaoTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private ConceptDao conceptDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  public void findExactMatchByCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();

    // test that we match just one source code
    CBCriteria sourceCriteria =
        new CBCriteria().domainId(domainId).count("100").standard(false).code("120");
    cbCriteriaDao.save(sourceCriteria);
    List<CBCriteria> exactMatchByCode = cbCriteriaDao.findExactMatchByCode(domainId, "120");
    assertEquals(1, exactMatchByCode.size());
    assertFalse(exactMatchByCode.get(0).getStandard());

    // test that we match both source and standard codes
    CBCriteria standardCriteria =
        new CBCriteria().domainId(domainId).count("100").standard(true).code("120");
    cbCriteriaDao.save(standardCriteria);
    exactMatchByCode = cbCriteriaDao.findExactMatchByCode(domainId, "120");
    assertEquals(2, exactMatchByCode.size());
    assertTrue(exactMatchByCode.get(0).getStandard());
  }

  @Test
  public void findCriteriaByDomainAndTypeAndCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    CBCriteria criteria =
        new CBCriteria()
            .domainId(domainId)
            .type(CriteriaType.ICD9CM.toString())
            .count("100")
            .standard(true)
            .code("001")
            .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndCode(
            domainId, CriteriaType.ICD9CM.toString(), Boolean.TRUE, "00", page);
    assertEquals(1, criteriaList.size());
    assertEquals(criteria, criteriaList.get(0));
  }

  @Test
  public void findCriteriaByDomainAndCode() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    CBCriteria criteria =
        new CBCriteria()
            .domainId(domainId)
            .count("100")
            .standard(true)
            .code("001")
            .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndCode(domainId, Boolean.TRUE, "001", page);
    assertEquals(1, criteriaList.size());
    assertEquals(criteria, criteriaList.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSynonyms() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    CBCriteria criteria =
        new CBCriteria()
            .domainId(domainId)
            .count("100")
            .standard(true)
            .synonyms("001[MEASUREMENT_rank1]");
    cbCriteriaDao.save(criteria);
    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> measurements =
        cbCriteriaDao.findCriteriaByDomainAndSynonyms(domainId, Boolean.TRUE, "001", page);
    assertEquals(1, measurements.size());
    assertEquals(criteria, measurements.get(0));
  }

  @Test
  public void findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    String icd9Type = CriteriaType.ICD9CM.toString();
    String icd10Type = CriteriaType.ICD10CM.toString();
    CBCriteria criteriaIcd9 =
        new CBCriteria()
            .domainId(domainId)
            .type(icd9Type)
            .hierarchy(true)
            .standard(false)
            .parentId(0);
    CBCriteria criteriaIcd10 =
        new CBCriteria()
            .domainId(domainId)
            .type(icd10Type)
            .hierarchy(true)
            .standard(false)
            .parentId(0);
    cbCriteriaDao.save(criteriaIcd9);
    cbCriteriaDao.save(criteriaIcd10);

    final CBCriteria actualIcd9 =
        cbCriteriaDao
            .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd9Type, false, 0L)
            .get(0);
    assertEquals(criteriaIcd9, actualIcd9);

    final CBCriteria actualIcd10 =
        cbCriteriaDao
            .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(domainId, icd10Type, false, 0L)
            .get(0);
    assertEquals(criteriaIcd10, actualIcd10);
  }

  @Test
  public void findCriteriaByDomainAndTypeOrderByIdAsc() throws Exception {
    String domainId = DomainType.PERSON.toString();
    String type = CriteriaType.RACE.toString();
    CBCriteria raceParent = new CBCriteria().domainId(domainId).type(type).name("Race").parentId(0);
    cbCriteriaDao.save(raceParent);
    CBCriteria raceChild1 =
        new CBCriteria().domainId(domainId).type(type).name("Asian").parentId(raceParent.getId());
    cbCriteriaDao.save(raceChild1);
    CBCriteria raceChild2 =
        new CBCriteria().domainId(domainId).type(type).name("White").parentId(raceParent.getId());
    cbCriteriaDao.save(raceChild2);

    final List<CBCriteria> demoList =
        cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domainId, type);
    assertEquals(3, demoList.size());
    assertEquals(raceParent, demoList.get(0));
    assertEquals(raceChild1, demoList.get(1));
    assertEquals(raceChild2, demoList.get(2));
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndCode() throws Exception {
    String domainId = DomainType.MEASUREMENT.toString();
    String type = CriteriaType.LOINC.toString();
    CBCriteria labCriteria =
        new CBCriteria()
            .domainId(domainId)
            .type(type)
            .hierarchy(true)
            .standard(true)
            .count("10")
            .code("LP123")
            .synonyms("+[MEASUREMENT_rank1]");
    CBCriteria labCriteria1 =
        new CBCriteria()
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
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
            domainId, type, true, "LP123", page);
    assertEquals(2, labs.size());
    assertEquals(labCriteria1, labs.get(0));
    assertEquals(labCriteria, labs.get(1));
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndSynonyms() throws Exception {
    String domainId = DomainType.CONDITION.toString();
    String type = CriteriaType.SNOMED.toString();
    CBCriteria snomedCriteria =
        new CBCriteria()
            .domainId(domainId)
            .type(type)
            .count("10")
            .hierarchy(true)
            .standard(true)
            .synonyms("myMatch[CONDITION_rank1]");
    cbCriteriaDao.save(snomedCriteria);

    PageRequest page = new PageRequest(0, 10);
    List<CBCriteria> conditions =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
            domainId, type, true, "myMatch", page);
    assertEquals(1, conditions.size());
    assertEquals(snomedCriteria, conditions.get(0));
  }

  @Test
  public void findDrugIngredientsByConceptId() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    Concept ingredient = new Concept().conceptId(1L).conceptClassId("Ingredient");
    conceptDao.save(ingredient);
    CBCriteria drugCriteriaIngredient =
        new CBCriteria()
            .domainId(DomainType.DRUG.toString())
            .type(CriteriaType.RXNORM.toString())
            .name("ACETAMIN")
            .selectable(true)
            .path("1.2.3.4")
            .conceptId("1")
            .synonyms("[DRUG_rank1]");
    cbCriteriaDao.save(drugCriteriaIngredient);

    List<CBCriteria> drugList = cbCriteriaDao.findDrugIngredientByConceptId("12345");
    assertEquals(1, drugList.size());
    assertEquals(drugCriteriaIngredient, drugList.get(0));
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findConceptId2ByConceptId1() throws Exception {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertEquals(1, cbCriteriaDao.findConceptId2ByConceptId1(12345L).get(0).intValue());
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .standard(true)
            .conceptId("1")
            .synonyms("[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(
        criteria,
        cbCriteriaDao
            .findStandardCriteriaByDomainAndConceptId(
                DomainType.CONDITION.toString(), true, Arrays.asList("1"))
            .get(0));
  }

  @Test
  public void findCriteriaParentsByDomainAndTypeAndParentConceptIds() throws Exception {
    String domain = DomainType.CONDITION.toString();
    String type = CriteriaType.SNOMED.toString();
    CBCriteria criteria =
        new CBCriteria()
            .domainId(domain)
            .type(type)
            .standard(false)
            .conceptId("1")
            .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    HashSet<String> parentConceptIds = new HashSet<>();
    parentConceptIds.add("1");
    List<CBCriteria> results =
        cbCriteriaDao.findCriteriaParentsByDomainAndTypeAndParentConceptIds(
            domain, type, false, parentConceptIds);
    assertEquals(criteria, results.get(0));
  }

  @Test
  public void findCriteriaLeavesAndParentsByPath() throws Exception {
    CBCriteria criteria =
        new CBCriteria()
            .domainId(DomainType.CONDITION.toString())
            .path("1.5.99")
            .synonyms("+[CONDITION_rank1]");
    cbCriteriaDao.save(criteria);
    assertEquals(
        criteria,
        cbCriteriaDao
            .findCriteriaLeavesAndParentsByDomainAndPath(DomainType.CONDITION.toString(), "5")
            .get(0));
  }
}
