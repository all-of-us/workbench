package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.utils.Booleans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DataSetDaoTest extends SpringTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private DbWorkspace workspace;

  @Autowired private CohortDao cohortDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private WorkspaceDao workspaceDao;

  @Before
  public void setup() {
    // FK constraint requires a real workspaceID
    workspace = workspaceDao.save(new DbWorkspace());
  }

  private DbDataset insertDatasetForGauge(boolean isInvalid, long workspaceId) {
    DbDataset dataset = new DbDataset();
    dataset.setName("Floyd");
    dataset.setVersion(1);
    dataset.setIncludesAllParticipants(true);
    dataset.setDescription("I dunno, just funny-lookin'.");
    dataset.setWorkspaceId(1);
    dataset.setInvalid(isInvalid);
    dataset.setCreatorId(101);
    dataset.setCreationTime(NOW);
    dataset.setCohortIds(Collections.emptyList());
    dataset.setConceptSetIds(Collections.emptyList());
    dataset.setValues(Collections.emptyList());
    dataset.setPrePackagedConceptSetEnum(
        Arrays.asList(PrePackagedConceptSetEnum.SURVEY, PrePackagedConceptSetEnum.PERSON));
    dataset.setWorkspaceId(workspaceId);
    return dataSetDao.save(dataset);
  }

  @Test
  public void testGauge() {
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(false, workspace.getWorkspaceId());

    final Map<Boolean, Long> map = dataSetDao.getInvalidToCountMap();
    assertThat(map).hasSize(Booleans.VALUE_STRINGS.size());
    assertThat(map.get(true)).isEqualTo(2L);
    assertThat(map.get(false)).isEqualTo(1L);
  }

  // TODO(jaycarlton): add coverage for concept sets and domains
  @Test
  public void testGetReportingDatasets() {
    final DbDataset dataset1 =
        dataSetDao.save(ReportingTestUtils.createDbDataset(workspace.getWorkspaceId()));
    DbDataset dataset2 = ReportingTestUtils.createDbDataset(workspace.getWorkspaceId());
    dataset2.setName("Name 2");
    DbCohort dbCohort1 = new DbCohort();
    dbCohort1.setName("Cohort");
    dbCohort1.setWorkspaceId(workspace.getWorkspaceId());
    dbCohort1 = cohortDao.save(dbCohort1);

    DbWorkspace workspace2 = new DbWorkspace();
    workspace2.setName("ws2");
    workspace2 = workspaceDao.save(workspace2);

    DbCohort dbCohort2 = new DbCohort();
    dbCohort2.setName("Cohort 2");
    dbCohort2.setDescription("Another Cohort");
    dbCohort2.setWorkspaceId(workspace2.getWorkspaceId());
    dbCohort2 = cohortDao.save(dbCohort2);

    dataset2.setCohortIds(ImmutableList.of(dbCohort1.getCohortId(), dbCohort2.getCohortId()));
    dataset2 = dataSetDao.save(dataset2);

    assertThat(dataset2.getCohortIds()).hasSize(2);
    assertThat(dataset2.getCohortIds())
        .containsExactly(dbCohort1.getCohortId(), dbCohort2.getCohortId());
    assertThat(dataset2.getCohortIds()).doesNotContain(0);
  }
}
