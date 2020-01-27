package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
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
                .synonyms("test")
                .path(surveyCriteria.getId() + ".1"));
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
    assertThat(criteriaList).containsExactly(surveyCriteria);
  }

  @Test
  public void findExactMatchByCode() {
    // test that we match both source and standard codes
    List<DbCriteria> exactMatchByCode =
        cbCriteriaDao.findExactMatchByCode(DomainType.CONDITION.toString(), "120");
    assertThat(exactMatchByCode).containsExactly(standardCriteria, sourceCriteria);
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
    assertThat(criteriaList).containsExactly(icd9Criteria);
  }

  @Test
  public void findCriteriaByDomainAndCode() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndCode(
            DomainType.CONDITION.toString(), Boolean.FALSE, "001", page);
    assertThat(criteriaList).containsExactly(icd9Criteria);
  }

  @Test
  public void findCriteriaByDomainAndSynonyms() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> measurements =
        cbCriteriaDao.findCriteriaByDomainAndSynonyms(
            DomainType.MEASUREMENT.toString(), Boolean.TRUE, "001", page);
    assertThat(measurements).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() {
    List<DbCriteria> actualIcd9s =
        cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
            DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L);
    assertThat(actualIcd9s).containsExactly(sourceCriteria, icd9Criteria);
    List<DbCriteria> actualIcd10s =
        cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
            DomainType.CONDITION.toString(), CriteriaType.ICD10CM.toString(), false, 0L);
    assertThat(actualIcd10s).containsExactly(icd10Criteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeOrderByIdAsc() {
    final List<DbCriteria> demoList =
        cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(
            DomainType.PERSON.toString(), CriteriaType.RACE.toString());
    assertThat(demoList).containsExactly(raceParent, raceAsian, raceWhite);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndCode() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> labs =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
            DomainType.MEASUREMENT.toString(), CriteriaType.LOINC.toString(), true, "LP123", page);
    assertThat(labs).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndSynonyms() {
    PageRequest page = new PageRequest(0, 10);
    List<DbCriteria> conditions =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
            DomainType.CONDITION.toString(), CriteriaType.SNOMED.toString(), true, "myMatch", page);
    assertThat(conditions).containsExactly(standardCriteria);
  }

  @Test
  public void findConceptId2ByConceptId1() {
    jdbcTemplate.execute(
        "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)");
    jdbcTemplate.execute(
        "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)");
    assertThat(cbCriteriaDao.findConceptId2ByConceptId1(12345L)).containsExactly(1);
    jdbcTemplate.execute("drop table cb_criteria_relationship");
  }

  @Test
  public void findStandardCriteriaByDomainAndConceptId() {
    assertThat(
            cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(
                DomainType.CONDITION.toString(), false, ImmutableList.of("1")))
        .containsExactly(icd10Criteria);
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
    assertThat(results).containsExactly(standardCriteria);
  }

  @Test
  public void findCriteriaLeavesAndParentsByPath() {
    assertThat(cbCriteriaDao.findCriteriaLeavesAndParentsByPath("5")).containsExactly(icd9Criteria);
  }

  @Test
  public void findGenderRaceEthnicity() {
    List<DbCriteria> criteriaList = cbCriteriaDao.findGenderRaceEthnicity();
    assertThat(criteriaList).containsExactly(raceAsian, raceWhite);
  }

  @Test
  public void findByDomainIdAndTypeAndParentIdNotIn() {
    Sort sort = new Sort(Direction.ASC, "name");
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
            DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort);
    assertThat(criteriaList).containsExactly(raceAsian, raceWhite).inOrder();

    // reverse
    sort = new Sort(Direction.DESC, "name");
    criteriaList =
        cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
            DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort);
    assertThat(criteriaList).containsExactly(raceWhite, raceAsian).inOrder();
  }

  @Test
  public void findMenuOptions() {
    List<DbMenuOption> options = cbCriteriaDao.findMenuOptions();
    DbMenuOption option1 = options.get(0);
    assertThat(option1.getDomain()).isEqualTo(DomainType.CONDITION.toString());
    assertThat(option1.getType()).isEqualTo("ICD10CM");
    assertThat(option1.getStandard()).isFalse();

    DbMenuOption option2 = options.get(1);
    assertThat(option2.getDomain()).isEqualTo(DomainType.CONDITION.toString());
    assertThat(option2.getType()).isEqualTo("ICD9CM");
    assertThat(option2.getStandard()).isFalse();

    DbMenuOption option3 = options.get(2);
    assertThat(option3.getDomain()).isEqualTo(DomainType.CONDITION.toString());
    assertThat(option3.getType()).isEqualTo("SNOMED");
    assertThat(option3.getStandard()).isTrue();

    DbMenuOption option4 = options.get(3);
    assertThat(option4.getDomain()).isEqualTo(DomainType.MEASUREMENT.toString());
    assertThat(option4.getType()).isEqualTo("LOINC");
    assertThat(option4.getStandard()).isTrue();

    DbMenuOption option5 = options.get(4);
    assertThat(option5.getDomain()).isEqualTo(DomainType.PERSON.toString());
    assertThat(option5.getType()).isEqualTo("RACE");
    assertThat(option5.getStandard()).isTrue();

    DbMenuOption option6 = options.get(5);
    assertThat(option6.getDomain()).isEqualTo(DomainType.SURVEY.toString());
    assertThat(option6.getType()).isEqualTo("PPI");
    assertThat(option6.getStandard()).isFalse();
  }

  @Test
  public void countSurveyBySearchTerm() {
    assertThat(cbCriteriaDao.countSurveyByKeyword("test")).isEqualTo(1);
  }

  @Test
  public void findSurveysKeyword() {
    PageRequest page = new PageRequest(0, 10);
    assertThat(cbCriteriaDao.findSurveys("test", null, page)).containsExactly(questionCriteria);
  }

  @Test
  public void countSurveyByName() {
    assertThat(cbCriteriaDao.countSurveyByName("The Basics")).isEqualTo(1);
  }

  @Test
  public void findSurveysByName() {
    PageRequest page = new PageRequest(0, 10);
    assertThat(cbCriteriaDao.findSurveys(null, "The Basics", page))
        .containsExactly(questionCriteria);
  }

  @Test
  public void findSurveysNoSurveyName() {
    PageRequest page = new PageRequest(0, 10);
    assertThat(cbCriteriaDao.findSurveys(null, null, page)).containsExactly(questionCriteria);
  }

  @Test
  public void countSurveys() {
    assertThat(cbCriteriaDao.countSurveys()).isEqualTo(1);
  }
}
