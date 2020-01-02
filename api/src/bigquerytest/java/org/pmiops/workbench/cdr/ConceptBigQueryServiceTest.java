package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.BigQueryTestService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.model.DbCdrVersion;
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

  @Autowired private ConceptDao conceptDao;

  @Autowired private DomainInfoDao domainInfoDao;

  @Autowired private SurveyModuleDao surveyModuleDao;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  private ConceptBigQueryService conceptBigQueryService;

  @Before
  public void setUp() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    ConceptService conceptService = new ConceptService(conceptDao, domainInfoDao, surveyModuleDao);
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
    conceptDao.save(
        new DbConcept()
            .conceptId(conceptId)
            .standardConcept(standardConceptValue)
            .conceptCode("concept" + conceptId)
            .conceptName("concept " + conceptId)
            .vocabularyId("V")
            .domainId("D"));
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
