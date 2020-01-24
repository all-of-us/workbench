package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
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
  private DbConcept concept1;
  private DbConcept concept2;
  private DbConcept physicalMeasurement;

  @Before
  public void setUp() {

    concept1 =
        conceptDao.save(
            new DbConcept()
                .conceptId(1L)
                .domainId(CommonStorageEnums.domainToDomainId(Domain.CONDITION))
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
                .domainId(CommonStorageEnums.domainToDomainId(Domain.CONDITION))
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
                .domainId(CommonStorageEnums.domainToDomainId(Domain.MEASUREMENT))
                .conceptName("Height")
                .vocabularyId(CriteriaType.PPI.toString())
                .conceptClassId("Clinical Observation")
                .conceptCode("020202")
                .standardConcept("")
                .count(2400L)
                .prevalence(0.0F)
                .sourceCountValue(2400L)
                .synonyms(ImmutableList.of("020202", "Height")));
  }

  @Test
  public void findAll() {
    List<DbConcept> concepts = (List<DbConcept>) conceptDao.findAll();
    assertThat(concepts).containsExactly(concept1, concept2, physicalMeasurement);
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
                CommonStorageEnums.domainToDomainId(Domain.PHYSICALMEASUREMENT)));
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
                CommonStorageEnums.domainToDomainId(Domain.PHYSICALMEASUREMENT)));
  }
}
