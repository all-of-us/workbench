package org.pmiops.workbench.cdr;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.api.BigQueryBaseTest;
import org.pmiops.workbench.api.BigQueryTestService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({BigQueryTestService.class, ConceptBigQueryService.class, TestJpaConfig.class})
public class ConceptBigQueryServiceTest extends BigQueryBaseTest {

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;
  @Autowired private ConceptBigQueryService conceptBigQueryService;

  @BeforeEach
  public void setUp() {
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
  }

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of("cb_criteria", "cb_search_all_events");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Test
  @Disabled("RW-5707")
  public void testGetConceptCountNoConceptsSaved() {
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder().addConceptId(6L).addStandard(true).build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder().addConceptId(10L).addStandard(false).build();
    assertThat(
            conceptBigQueryService.getParticipantCountForConcepts(
                Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .isEqualTo(1);
  }
}
