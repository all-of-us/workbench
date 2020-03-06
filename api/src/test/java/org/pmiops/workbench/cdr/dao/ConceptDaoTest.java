package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptDaoTest {

  @Autowired ConceptDao conceptDao;
  @Autowired CBCriteriaDao cbCriteriaDao;
  private DbConcept concept1;
  private DbConcept concept2;
  private DbConcept physicalMeasurement;
  private DbConcept surveyConcept;

  @Before
  public void setUp() {

    concept1 =
        conceptDao.save(
            new DbConcept()
                .conceptId(1L)
                .domainId(DbStorageEnums.domainToDomainId(Domain.CONDITION))
                .conceptName("Personal history of malignant neoplasm of breast")
                .vocabularyId(CriteriaType.ICD9CM.toString())
                .conceptClassId("4-char billing code")
                .conceptCode("Z85")
                .standardConcept("")
                .count(3094L)
                .prevalence(0.0F)
                .sourceCountValue(3094L)
                .synonyms(
                    ImmutableList.of(
                        "35225339",
                        "Personal history of malignant neoplasm of breast|Personal history of malignant neoplasm of breast")));
    concept2 =
        conceptDao.save(
            new DbConcept()
                .conceptId(2L)
                .domainId(DbStorageEnums.domainToDomainId(Domain.CONDITION))
                .conceptName("Personal history of malignant neoplasm")
                .vocabularyId(CriteriaType.SNOMED.toString())
                .conceptClassId("4-char billing code")
                .conceptCode("Z86")
                .standardConcept("S")
                .count(3094L)
                .prevalence(0.0F)
                .sourceCountValue(3094L)
                .synonyms(
                    ImmutableList.of(
                        "35225339",
                        "Personal history of malignant neoplasm of breast|Personal history of malignant neoplasm of breast")));
    physicalMeasurement =
        conceptDao.save(
            new DbConcept()
                .conceptId(3L)
                .domainId(DbStorageEnums.domainToDomainId(Domain.MEASUREMENT))
                .conceptName("Height")
                .vocabularyId(CriteriaType.PPI.toString())
                .conceptClassId("Clinical Observation")
                .conceptCode("020202")
                .standardConcept("")
                .count(2400L)
                .prevalence(0.0F)
                .sourceCountValue(2400L)
                .synonyms(ImmutableList.of("020202", "Height")));

    surveyConcept =
        conceptDao.save(
            new DbConcept()
                .conceptId(4L)
                .domainId(DbStorageEnums.domainToDomainId(Domain.OBSERVATION))
                .conceptName("survey")
                .vocabularyId(CriteriaType.PPI.toString())
                .conceptClassId("Question")
                .conceptCode("Income_AnnualIncome")
                .standardConcept("")
                .count(200L)
                .prevalence(0.0F)
                .sourceCountValue(200L)
                .synonyms(ImmutableList.of("4", "question")));

    DbCriteria surveyCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(DomainType.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.SURVEY.toString())
                .addGroup(true)
                .addStandard(false)
                .addSelectable(true)
                .addName("The Basics")
                .build());

    DbCriteria questionCriteria =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(DomainType.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addGroup(true)
                .addStandard(false)
                .addSelectable(true)
                .addParentId(surveyCriteria.getId())
                .addConceptId("4")
                .addSynonyms("test")
                .addPath(surveyCriteria.getId() + ".1")
                .build());

    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(DomainType.SURVEY.toString())
            .addType(CriteriaType.PPI.toString())
            .addSubtype(CriteriaSubType.ANSWER.toString())
            .addGroup(false)
            .addStandard(false)
            .addSelectable(true)
            .addParentId(questionCriteria.getId())
            .addConceptId("4")
            .addSynonyms("test")
            .addPath(questionCriteria.getPath() + ".1")
            .build());
  }

  @Test
  public void findAllEmptyResult() {
    Iterable concepts = conceptDao.findAll(ImmutableList.of(9999L, 8888L));
    assertThat(concepts).isEmpty();
  }

  @Test
  public void findAll() {
    Iterable concepts = conceptDao.findAll();
    assertThat(concepts).containsExactly(concept1, concept2, physicalMeasurement, surveyConcept);
  }

  @Test
  public void findStandardConceptsWithKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts("history", ImmutableList.of("S", "C"), Domain.CONDITION, page);
    assertThat(concepts).containsExactly(concept2);
  }

  @Test
  public void findStandardConceptsWithoutKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(null, ImmutableList.of("S", "C"), Domain.CONDITION, page);
    assertThat(concepts).containsExactly(concept2);
  }

  @Test
  public void findAllConceptsWithKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts("history", ImmutableList.of("S", "C", ""), Domain.CONDITION, page);
    assertThat(concepts).containsExactly(concept1, concept2);
  }

  @Test
  public void findAllConceptsWithoutKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(null, ImmutableList.of("S", "C", ""), Domain.CONDITION, page);
    assertThat(concepts).containsExactly(concept1, concept2);
  }

  @Test
  public void findStandardConceptsPhysicalMeasurementsWithKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(
            "height", ImmutableList.of("S", "C"), Domain.PHYSICALMEASUREMENT, page);
    assertThat(concepts).hasSize(0);
  }

  @Test
  public void findStandardConceptsPhysicalMeasurementsWithoutKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(null, ImmutableList.of("S", "C"), Domain.PHYSICALMEASUREMENT, page);
    assertThat(concepts).hasSize(0);
  }

  @Test
  public void findAllConceptsPhysicalMeasurementsWithKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(
            "Height", ImmutableList.of("S", "C", ""), Domain.PHYSICALMEASUREMENT, page);
    assertThat(concepts)
        .containsExactly(
            physicalMeasurement.domainId(
                DbStorageEnums.domainToDomainId(Domain.PHYSICALMEASUREMENT)));
  }

  @Test
  public void findAllConceptsPhysicalMeasurementsWithoutKeyword() {
    Pageable page = new PageRequest(0, 100, new Sort(Direction.DESC, "countValue"));
    Slice<DbConcept> concepts =
        conceptDao.findConcepts(
            null, ImmutableList.of("S", "C", ""), Domain.PHYSICALMEASUREMENT, page);
    assertThat(concepts)
        .containsExactly(
            physicalMeasurement.domainId(
                DbStorageEnums.domainToDomainId(Domain.PHYSICALMEASUREMENT)));
  }

  @Test
  public void countSurveyByName() {
    assertThat(conceptDao.countSurveyByName("The Basics")).isEqualTo(1);
  }

  @Test
  public void findSurveysByName() {
    PageRequest page = new PageRequest(0, 10);
    assertThat(conceptDao.findSurveys(null, "The Basics", page)).containsExactly(surveyConcept);
  }
}
