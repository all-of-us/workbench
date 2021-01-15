package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.BigQueryTestService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
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

  @Autowired private BigQueryService bigQueryService;

  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  private ConceptBigQueryService conceptBigQueryService;

  private DbConceptSetConceptId dbConceptSetConceptId1;
  private DbConceptSetConceptId dbConceptSetConceptId2;
  private DbConceptSetConceptId dbConceptSetConceptId3;
  private DbConceptSetConceptId dbConceptSetConceptId4;

  @Before
  public void setUp() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(true).build();
    dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder().addConceptId(6L).addStandard(true).build();
    dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder().addConceptId(13L).addStandard(true).build();
    dbConceptSetConceptId4 =
        DbConceptSetConceptId.builder().addConceptId(192819L).addStandard(true).build();

    conceptBigQueryService =
        new ConceptBigQueryService(bigQueryService, cdrBigQuerySchemaConfigService);

    conceptDao.deleteAll();
  }

  @Test
  public void testGetConceptCountNoConceptsSaved() {
    assertThat(
            conceptBigQueryService.getParticipantCountForConcepts(
                Domain.CONDITION,
                "condition_occurrence",
                ImmutableSet.of(
                    dbConceptSetConceptId1,
                    dbConceptSetConceptId2,
                    dbConceptSetConceptId3,
                    dbConceptSetConceptId4)))
        .isEqualTo(1);
  }

  @Test
  @Ignore("RW-5707")
  public void testGetConceptCountConceptsSaved() {
    saveConcept(1L, "S");
    saveConcept(6L, null);
    saveConcept(13L, null);
    saveConcept(192819L, "C");

    assertThat(
            conceptBigQueryService.getParticipantCountForConcepts(
                Domain.CONDITION,
                "condition_occurrence",
                ImmutableSet.of(
                    dbConceptSetConceptId1,
                    dbConceptSetConceptId2,
                    dbConceptSetConceptId3,
                    dbConceptSetConceptId4)))
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
    return ImmutableList.of("condition_occurrence", "cb_criteria", "cb_search_all_events");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }
}
