package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbMenuOption;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.FilterColumns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBCriteriaDaoTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  private DbCriteria surveyCriteria;
  private DbCriteria questionCriteria;
  private DbCriteria sourceCriteria;
  private DbCriteria standardCriteria;
  private DbCriteria icd9Criteria;
  private DbCriteria icd10Criteria;
  private DbCriteria measurementCriteria;
  private DbCriteria raceParent;
  private DbCriteria raceAsian;
  private DbCriteria raceWhite;

  @Before
  public void setUp() {
    surveyCriteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.SURVEY.toString())
                .group(false)
                .standard(false)
                .selectable(true)
                .name("The Basics"));
    questionCriteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.QUESTION.toString())
                .group(false)
                .standard(false)
                .selectable(true)
                .parentId(surveyCriteria.getId())
                .synonyms("test"));
    sourceCriteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .count("100")
                .standard(false)
                .code("120"));
    standardCriteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .count("100")
                .hierarchy(true)
                .conceptId("1")
                .standard(true)
                .code("120")
                .synonyms("myMatch[CONDITION_rank1]"));
    icd9Criteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .count("100")
                .standard(false)
                .code("001")
                .synonyms("+[CONDITION_rank1]")
                .path("1.5.99"));
    icd10Criteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD10CM.toString())
                .count("100")
                .standard(false)
                .conceptId("1")
                .code("122")
                .synonyms("+[CONDITION_rank1]"));
    measurementCriteria =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.MEASUREMENT.toString())
                .type(CriteriaType.LOINC.toString())
                .count("100")
                .hierarchy(true)
                .standard(true)
                .code("LP123")
                .synonyms("001[MEASUREMENT_rank1]"));
    raceParent =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.PERSON.toString())
                .type(CriteriaType.RACE.toString())
                .name("Race")
                .standard(true)
                .parentId(0));
    raceAsian =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.PERSON.toString())
                .type(CriteriaType.RACE.toString())
                .name("Asian")
                .standard(true)
                .parentId(raceParent.getId()));
    raceWhite =
        cbCriteriaDao.save(
            new DbCriteria()
                .domainId(DomainType.PERSON.toString())
                .type(CriteriaType.RACE.toString())
                .name("White")
                .standard(true)
                .parentId(raceParent.getId()));
  }

  @Test
  public void findCriteriaLeavesByDomainAndTypeAndSubtype() {
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaLeavesByDomainAndTypeAndSubtype(
            DomainType.SURVEY.toString(),
            CriteriaType.PPI.toString(),
            CriteriaSubType.SURVEY.toString());
    assertEquals(1, criteriaList.size());
    assertEquals(surveyCriteria, criteriaList.get(0));
  }

  @Test
  public void findExactMatchByCode() {
    // test that we match both source and standard codes
    List<DbCriteria> exactMatchByCode =
        cbCriteriaDao.findExactMatchByCode(DomainType.CONDITION.toString(), "120");
    assertEquals(2, exactMatchByCode.size());
    assertEquals(standardCriteria, exactMatchByCode.get(0));
    assertEquals(sourceCriteria, exactMatchByCode.get(1));
  }

  @Test
  public void findCriteriaByDomainAndTypeAndCode() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndCode(
            DomainType.CONDITION.toString(),
            CriteriaType.ICD9CM.toString(),
            Boolean.FALSE,
            "00",
            page);
    assertEquals(1, criteriaList.size());
    assertEquals(icd9Criteria, criteriaList.get(0));
  }

  @Test
  public void findCriteriaByDomainAndCode() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndCode(
            DomainType.CONDITION.toString(), Boolean.FALSE, "001", page);
    assertEquals(1, criteriaList.size());
    assertEquals(icd9Criteria, criteriaList.get(0));
  }

  @Test
  public void findCriteriaByDomainAndSynonyms() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> measurements =
        cbCriteriaDao.findCriteriaByDomainAndSynonyms(
            DomainType.MEASUREMENT.toString(), Boolean.TRUE, "001", page);
    assertEquals(1, measurements.size());
    assertEquals(measurementCriteria, measurements.get(0));
  }

  @Test
  public void findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() {
    DbCriteria actualIcd9 =
        cbCriteriaDao
            .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
                DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)
            .get(0);
    assertEquals(sourceCriteria, actualIcd9);

    DbCriteria actualIcd10 =
        cbCriteriaDao
            .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
                DomainType.CONDITION.toString(), CriteriaType.ICD10CM.toString(), false, 0L)
            .get(0);
    assertEquals(icd10Criteria, actualIcd10);
  }

  @Test
  public void findCriteriaByDomainAndTypeOrderByIdAsc() {
    final List<DbCriteria> demoList =
        cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(
            DomainType.PERSON.toString(), CriteriaType.RACE.toString());
    assertEquals(3, demoList.size());
    assertEquals(raceParent, demoList.get(0));
    assertEquals(raceAsian, demoList.get(1));
    assertEquals(raceWhite, demoList.get(2));
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndCode() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> labs =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
            DomainType.MEASUREMENT.toString(), CriteriaType.LOINC.toString(), true, "LP123", page);
    assertEquals(1, labs.size());
    assertEquals(measurementCriteria, labs.get(0));
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndSynonyms() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> conditions =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
            DomainType.CONDITION.toString(), CriteriaType.SNOMED.toString(), true, "myMatch", page);
    assertEquals(1, conditions.size());
    assertEquals(standardCriteria, conditions.get(0));
  }

  @Test
  public void findConceptId2ByConceptId1() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertEquals(1, cbCriteriaDao.findConceptId2ByConceptId1(12345L).get(0).intValue());
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() {
    assertEquals(
        icd10Criteria,
        cbCriteriaDao
            .findStandardCriteriaByDomainAndConceptId(
                DomainType.CONDITION.toString(), false, Arrays.asList("1"))
            .get(0));
  }

  @Test
  public void findCriteriaParentsByDomainAndTypeAndParentConceptIds() {
    HashSet<String> parentConceptIds = new HashSet<>();
    parentConceptIds.add("1");
    List<DbCriteria> results =
        cbCriteriaDao.findCriteriaParentsByDomainAndTypeAndParentConceptIds(
            DomainType.CONDITION.toString(),
            CriteriaType.SNOMED.toString(),
            true,
            parentConceptIds);
    assertEquals(standardCriteria, results.get(0));
  }

  @Test
  public void findCriteriaLeavesAndParentsByPath() {
    assertEquals(icd9Criteria, cbCriteriaDao.findCriteriaLeavesAndParentsByPath("5").get(0));
  }

  @Test
  public void findGenderRaceEthnicity() {
    List<DbCriteria> criteriaList = cbCriteriaDao.findGenderRaceEthnicity();
    assertEquals(2, criteriaList.size());
    assertTrue(criteriaList.contains(raceAsian));
    assertTrue(criteriaList.contains(raceWhite));
  }

  @Test
  public void findByDomainIdAndTypeAndParentIdNotIn() {
    Sort sort = new Sort(Direction.ASC, "name");
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
            DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort);
    assertEquals(2, criteriaList.size());
    assertEquals(raceAsian, criteriaList.get(0));
    assertEquals(raceWhite, criteriaList.get(1));

    // reverse
    sort = new Sort(Direction.DESC, "name");
    criteriaList =
        cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
            DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort);
    assertEquals(2, criteriaList.size());
    assertEquals(raceWhite, criteriaList.get(0));
    assertEquals(raceAsian, criteriaList.get(1));
  }

  @Test
  public void findMenuOptions() {
    List<DbMenuOption> options = cbCriteriaDao.findMenuOptions();
    assertEquals(6, options.size());

    DbMenuOption option = options.get(0);
    assertEquals(DomainType.CONDITION.toString(), option.getDomain());
    assertEquals("ICD10CM", option.getType());
    assertFalse(option.getStandard());

    option = options.get(1);
    assertEquals(DomainType.CONDITION.toString(), option.getDomain());
    assertEquals("ICD9CM", option.getType());
    assertFalse(option.getStandard());

    option = options.get(2);
    assertEquals(DomainType.CONDITION.toString(), option.getDomain());
    assertEquals("SNOMED", option.getType());
    assertTrue(option.getStandard());

    option = options.get(3);
    assertEquals(DomainType.MEASUREMENT.toString(), option.getDomain());
    assertEquals("LOINC", option.getType());
    assertTrue(option.getStandard());

    option = options.get(4);
    assertEquals(DomainType.PERSON.toString(), option.getDomain());
    assertEquals("RACE", option.getType());
    assertTrue(option.getStandard());

    option = options.get(5);
    assertEquals(DomainType.SURVEY.toString(), option.getDomain());
    assertEquals("PPI", option.getType());
    assertFalse(option.getStandard());
  }

  @Test
  public void findSurveyCountByTerm() {
    assertEquals(1, cbCriteriaDao.findSurveyCountByTerm("test"));
  }

  @Test
  public void findSurveyCountBySurveyName() {
    assertEquals(1, cbCriteriaDao.findSurveyCountBySurveyName("The Basics"));
  }
}
