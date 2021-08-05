package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.FilterColumns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBCriteriaDaoTest extends SpringTest {

  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private JdbcTemplate jdbcTemplate;
  private DbCriteria surveyCriteria;
  private DbCriteria sourceCriteria;
  private DbCriteria standardCriteria;
  private DbCriteria icd9Criteria;
  private DbCriteria icd10Criteria;
  private DbCriteria measurementCriteria;
  private DbCriteria raceAsian;
  private DbCriteria raceWhite;
  private DbCriteria gender;
  private DbCriteria ethnicity;
  private DbCriteria sexAtBirth;

  @BeforeEach
  public void setUp() {
    surveyCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addGroup(false)
                .addConceptId("1")
                .addStandard(false)
                .addSelectable(true)
                .addName("The Basics")
                .build());
    // adding a survey answer
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.ANSWER.toString())
            .addGroup(false)
            .addConceptId("1")
            .addStandard(false)
            .addSelectable(true)
            .addName("Answer")
            .addPath(String.valueOf(surveyCriteria.getId()))
            .addFullText("term[SURVEY_rank1]")
            .build());
    sourceCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD9CM.toString())
                .addCount(100L)
                .addStandard(false)
                .addSelectable(true)
                .addCode("120")
                .addConceptId("12")
                .addFullText("term[CONDITION_rank1]")
                .build());
    standardCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.SNOMED.toString())
                .addCount(100L)
                .addHierarchy(true)
                .addConceptId("1")
                .addStandard(true)
                .addSelectable(true)
                .addCode("120")
                .addFullText("myMatch[CONDITION_rank1]")
                .build());
    icd9Criteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD9CM.toString())
                .addCount(100L)
                .addStandard(false)
                .addCode("001")
                .addFullText("+[CONDITION_rank1]")
                .addPath("1.5.99")
                .build());
    icd10Criteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.CONDITION.toString())
                .addType(CriteriaType.ICD10CM.toString())
                .addCount(100L)
                .addStandard(false)
                .addConceptId("1")
                .addCode("122")
                .addFullText("+[CONDITION_rank1]")
                .build());
    measurementCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.MEASUREMENT.toString())
                .addType(CriteriaType.LOINC.toString())
                .addCount(100L)
                .addHierarchy(true)
                .addStandard(true)
                .addCode("LP123")
                .addSelectable(true)
                .addFullText("001[MEASUREMENT_rank1]")
                .build());
    raceAsian =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PERSON.toString())
                .addType(CriteriaType.RACE.toString())
                .addName("Asian")
                .addStandard(true)
                .build());
    raceWhite =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PERSON.toString())
                .addType(CriteriaType.RACE.toString())
                .addName("White")
                .addStandard(true)
                .build());
    gender =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PERSON.toString())
                .addType(CriteriaType.GENDER.toString())
                .addName("Male")
                .addStandard(true)
                .addParentId(1)
                .build());
    ethnicity =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PERSON.toString())
                .addType(CriteriaType.ETHNICITY.toString())
                .addName("Not Hispanic or Latino")
                .addStandard(true)
                .addParentId(1)
                .build());
    sexAtBirth =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.PERSON.toString())
                .addType(CriteriaType.SEX.toString())
                .addName("Male")
                .addStandard(true)
                .addParentId(1)
                .build());
  }

  @Test
  public void findSurveyId() {
    assertThat(cbCriteriaDao.findSurveyId("The Basics")).isEqualTo(surveyCriteria.getId());
  }

  @Test
  public void findSurveyQuestionByPathAndTerm() {
    PageRequest pageRequest = PageRequest.of(0, 100);
    assertThat(
            cbCriteriaDao
                .findSurveyQuestionByPathAndTerm(surveyCriteria.getId(), "term", pageRequest)
                .getContent()
                .get(0))
        .isEqualTo(surveyCriteria);
  }

  @Test
  public void findCriteriaByDomainIdAndConceptIds() {
    assertThat(
            cbCriteriaDao.findCriteriaByDomainIdAndStandardAndConceptIds(
                "CONDITION", false, ImmutableList.of("12")))
        .containsExactly(sourceCriteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndCode() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> criteriaList =
        cbCriteriaDao
            .findCriteriaByDomainAndTypeAndCode(Domain.CONDITION.toString(), "00", page)
            .getContent();
    assertThat(criteriaList).containsExactly(icd9Criteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndCodeAndStandard() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> criteriaList =
        cbCriteriaDao
            .findCriteriaByDomainAndTypeAndCodeAndStandard(
                Domain.CONDITION.toString(), "00", true, page)
            .getContent();
    assertThat(criteriaList).isEmpty();
    criteriaList =
        cbCriteriaDao
            .findCriteriaByDomainAndTypeAndCodeAndStandard(
                Domain.CONDITION.toString(), "00", false, page)
            .getContent();
    assertThat(criteriaList).containsExactly(icd9Criteria);
  }

  @Test
  public void findCriteriaByDomainAndFullText() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> measurements =
        cbCriteriaDao
            .findCriteriaByDomainAndFullText(Domain.MEASUREMENT.toString(), "001", page)
            .getContent();
    assertThat(measurements).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaByDomainAndFullTextAndStandard() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> measurements =
        cbCriteriaDao
            .findCriteriaByDomainAndFullTextAndStandard(
                Domain.MEASUREMENT.toString(), "001", false, page)
            .getContent();
    assertThat(measurements).isEmpty();
    measurements =
        cbCriteriaDao
            .findCriteriaByDomainAndFullTextAndStandard(
                Domain.MEASUREMENT.toString(), "001", true, page)
            .getContent();
    assertThat(measurements).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaTopCounts() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> measurements =
        cbCriteriaDao.findCriteriaTopCounts(Domain.MEASUREMENT.toString(), page).getContent();
    assertThat(measurements).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaTopCountsByStandard() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> conditions =
        cbCriteriaDao
            .findCriteriaTopCountsByStandard(Domain.CONDITION.toString(), true, page)
            .getContent();
    assertThat(conditions).containsExactly(standardCriteria);
    conditions =
        cbCriteriaDao
            .findCriteriaTopCountsByStandard(Domain.CONDITION.toString(), false, page)
            .getContent();
    assertThat(conditions).containsExactly(sourceCriteria);
  }

  @Test
  public void findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() {
    List<DbCriteria> actualIcd9s =
        cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
            Domain.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L);
    assertThat(actualIcd9s).containsExactly(sourceCriteria, icd9Criteria);
    List<DbCriteria> actualIcd10s =
        cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
            Domain.CONDITION.toString(), CriteriaType.ICD10CM.toString(), false, 0L);
    assertThat(actualIcd10s).containsExactly(icd10Criteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeOrderByIdAsc() {
    final List<DbCriteria> demoList =
        cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(
            Domain.PERSON.toString(), CriteriaType.RACE.toString());
    assertThat(demoList).containsExactly(raceAsian, raceWhite);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndCode() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> labs =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
            Domain.MEASUREMENT.toString(), CriteriaType.LOINC.toString(), true, "LP123", page);
    assertThat(labs).containsExactly(measurementCriteria);
  }

  @Test
  public void findCriteriaByDomainAndTypeAndStandardAndFullText() {
    PageRequest page = PageRequest.of(0, 10);
    List<DbCriteria> conditions =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndFullText(
            Domain.CONDITION.toString(), CriteriaType.SNOMED.toString(), true, "myMatch", page);
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
                Domain.CONDITION.toString(), false, ImmutableList.of("1")))
        .containsExactly(icd10Criteria);
  }

  @Test
  public void findParticipantDemographics() {
    List<DbCriteria> criteriaList = cbCriteriaDao.findAllDemographics();
    assertThat(criteriaList).containsExactly(gender, sexAtBirth, ethnicity, raceAsian, raceWhite);
  }

  @Test
  public void findByDomainIdAndType() {
    Sort sort = Sort.by(Direction.ASC, "name");
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findByDomainIdAndType(
            Domain.PERSON.toString(), FilterColumns.RACE.toString(), sort);
    assertThat(criteriaList).containsExactly(raceAsian, raceWhite).inOrder();

    // reverse
    sort = Sort.by(Direction.DESC, "name");
    criteriaList =
        cbCriteriaDao.findByDomainIdAndType(
            Domain.PERSON.toString(), FilterColumns.RACE.toString(), sort);
    assertThat(criteriaList).containsExactly(raceWhite, raceAsian).inOrder();
  }

  @Test
  public void findDomainCount() {
    assertThat(cbCriteriaDao.findDomainCount("term", Domain.CONDITION.toString())).isEqualTo(1);
  }

  @Test
  public void findDomainCountOnCode() {
    assertThat(cbCriteriaDao.findDomainCountOnCode("120", Domain.CONDITION.toString()))
        .isEqualTo(2);
  }

  @Test
  public void findSurveyVersionByQuestionConceptId() {
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_version_concept_id integer, survey_concept_id integer, display_name varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "create table cb_survey_attribute(id integer, question_concept_id integer, answer_concept_id integer, survey_version_concept_id integer, item_count integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (100, 1333342, 'May 2020', 1)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (101, 1333342, 'June 2020', 2)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (102, 1333342, 'July 2020', 3)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (1, 715713, 0, 100, 291)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (2, 715713, 0, 101, 148)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (3, 715713, 0, 102, 150)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (1, 715713, 1, 100, 491)");
    List<DbSurveyVersion> dbSurveyVersions =
        cbCriteriaDao.findSurveyVersionByQuestionConceptIdAndAnswerConceptId(1333342L, 715713L, 0L);
    assertThat(dbSurveyVersions).hasSize(3);
    assertThat(dbSurveyVersions.get(0).getSurveyVersionConceptId()).isEqualTo(100);
    assertThat(dbSurveyVersions.get(0).getDisplayName()).isEqualTo("May 2020");
    assertThat(dbSurveyVersions.get(0).getItemCount()).isEqualTo(291);
    assertThat(dbSurveyVersions.get(1).getSurveyVersionConceptId()).isEqualTo(101);
    assertThat(dbSurveyVersions.get(1).getDisplayName()).isEqualTo("June 2020");
    assertThat(dbSurveyVersions.get(1).getItemCount()).isEqualTo(148);
    assertThat(dbSurveyVersions.get(2).getSurveyVersionConceptId()).isEqualTo(102);
    assertThat(dbSurveyVersions.get(2).getDisplayName()).isEqualTo("July 2020");
    assertThat(dbSurveyVersions.get(2).getItemCount()).isEqualTo(150);
    jdbcTemplate.execute("drop table cb_survey_version");
    jdbcTemplate.execute("drop table cb_survey_attribute");
  }

  @Test
  public void findSurveyVersionByQuestionConceptIdAndAnswerConceptId() {
    jdbcTemplate.execute(
        "create table cb_survey_version(survey_version_concept_id integer, survey_concept_id integer, display_name varchar(50), display_order integer)");
    jdbcTemplate.execute(
        "create table cb_survey_attribute(id integer, question_concept_id integer, answer_concept_id integer, survey_version_concept_id integer, item_count integer)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (100, 1333342, 'May 2020', 1)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (101, 1333342, 'June 2020', 2)");
    jdbcTemplate.execute(
        "insert into cb_survey_version(survey_version_concept_id, survey_concept_id, display_name, display_order) values (102, 1333342, 'July 2020', 3)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (1, 715713, 0, 100, 291)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (2, 715713, 0, 101, 148)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (3, 715713, 0, 102, 150)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (4, 715713, 903096, 100, 154)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (5, 715713, 903096, 101, 82)");
    jdbcTemplate.execute(
        "insert into cb_survey_attribute(id, question_concept_id, answer_concept_id, survey_version_concept_id, item_count) values (6, 715713, 903096, 102, 31)");
    List<DbSurveyVersion> dbSurveyVersions =
        cbCriteriaDao.findSurveyVersionByQuestionConceptIdAndAnswerConceptId(1333342L, 715713L, 0L);
    assertThat(dbSurveyVersions).hasSize(3);
    assertThat(dbSurveyVersions.get(0).getSurveyVersionConceptId()).isEqualTo(100);
    assertThat(dbSurveyVersions.get(0).getDisplayName()).isEqualTo("May 2020");
    assertThat(dbSurveyVersions.get(0).getItemCount()).isEqualTo(291);
    assertThat(dbSurveyVersions.get(1).getSurveyVersionConceptId()).isEqualTo(101);
    assertThat(dbSurveyVersions.get(1).getDisplayName()).isEqualTo("June 2020");
    assertThat(dbSurveyVersions.get(1).getItemCount()).isEqualTo(148);
    assertThat(dbSurveyVersions.get(2).getSurveyVersionConceptId()).isEqualTo(102);
    assertThat(dbSurveyVersions.get(2).getDisplayName()).isEqualTo("July 2020");
    assertThat(dbSurveyVersions.get(2).getItemCount()).isEqualTo(150);
    jdbcTemplate.execute("drop table cb_survey_version");
    jdbcTemplate.execute("drop table cb_survey_attribute");
  }
}
