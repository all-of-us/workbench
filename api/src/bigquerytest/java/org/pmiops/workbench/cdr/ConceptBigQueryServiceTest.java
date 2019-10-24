package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.BigQueryTestService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({
  BigQueryTestService.class,
  TestBigQueryCdrSchemaConfig.class,
  TestJpaConfig.class,
  CdrBigQuerySchemaConfigService.class
})
public class ConceptBigQueryServiceTest extends BigQueryBaseTest {

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @PersistenceContext private EntityManager entityManager;

  @Autowired private ConceptDao conceptDao;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  private ConceptBigQueryService conceptBigQueryService;

  @Before
  public void setUp() {
    CdrVersion cdrVersionEntity = new CdrVersion();
    cdrVersionEntity.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersionEntity.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersionEntity);

    ConceptService conceptService = new ConceptService(entityManager, conceptDao);
    conceptBigQueryService =
        new ConceptBigQueryService(bigQueryService, cdrBigQuerySchemaConfigService, conceptService);

    conceptDao.deleteAll();
  }

  @Test
  public void testGetConceptCountNoConceptsSaved() {
    assertThat(
            conceptBigQueryService.getParticipantCountForConcepts(
                "condition_occurrence", ImmutableSet.of(1L, 6L, 13L, 192819L)))
        .isEqualTo(0);
  }

  @Test
  public void testGetConceptCountConceptsSaved() {
    saveConcept(1L, "S");
    saveConcept(6L, null);
    saveConcept(13L, null);
    saveConcept(192819L, "C");

    assertThat(
            conceptBigQueryService.getParticipantCountForConcepts(
                "condition_occurrence", ImmutableSet.of(1L, 6L, 13L, 192819L)))
        .isEqualTo(2);
  }

  private void saveConcept(long conceptId, String standardConceptValue) {
    Concept concept = new Concept();
    concept.setConceptId(conceptId);
    concept.setStandardConcept(standardConceptValue);
    concept.setConceptCode("concept" + conceptId);
    concept.setConceptName("concept " + conceptId);
    concept.setVocabularyId("V");
    concept.setDomainId("D");
    conceptDao.save(concept);
  }

  @Override
  public List<String> getTableNames() {
    return Arrays.asList("condition_occurrence");
  }

  @Override
  public String getTestDataDirectory() {
    return MATERIALIZED_DATA;
  }
}
