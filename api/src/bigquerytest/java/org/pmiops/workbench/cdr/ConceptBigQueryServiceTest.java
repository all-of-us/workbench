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
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({BigQueryService.class, TestBigQueryCdrSchemaConfig.class, TestJpaConfig.class,
    ConceptBigQueryService.class})
public class ConceptBigQueryServiceTest extends BigQueryBaseTest {

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired
  private ConceptBigQueryService conceptBigQueryService;

  @Before
  public void setUp() {
    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
  }

  @Test
  public void testGetConceptCount() {
    assertThat(conceptBigQueryService.getParticipantCountForConcepts("condition_occurrence",
        ImmutableSet.of(1L, 6L, 13L, 44829697L))).isEqualTo(2);
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
