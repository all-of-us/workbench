package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDataset;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDatasetCohort;
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
public class DataSetDaoTest {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private DbWorkspace workspace;

  @Autowired private DataSetDao dataSetDao;
  @Autowired private DatasetJoinTableNativeDao datasetJoinTableNativeDao;
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
  public void testGague() {
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(false, workspace.getWorkspaceId());

    final Map<Boolean, Long> map = dataSetDao.getInvalidToCountMap();
    assertThat(map).hasSize(Booleans.VALUE_STRINGS.size());
    assertThat(map.get(true)).isEqualTo(2L);
    assertThat(map.get(false)).isEqualTo(1L);
  }

  @Test
  public void testGetReportingDatasets() {
    final DbDataset dataset1 =
        dataSetDao.save(ReportingTestUtils.createDbDataset(workspace.getWorkspaceId()));
    DbDataset dataset2 = ReportingTestUtils.createDbDataset(workspace.getWorkspaceId());
    dataset2.setName("Name 2");
    final DbCohort dbCohort = new DbCohort();
    dbCohort.setName("Cohort");
    dataset2.setCohortIds(Collections.singletonList(dbCohort.getCohortId()));
    dataset2 = dataSetDao.save(dataset2);

    final List<ProjectedReportingDataset> projections = dataSetDao.getReportingDatasets();
    assertThat(projections).hasSize(2);
    ReportingTestUtils.assertDatasetFields(projections.get(0));
    assertThat(projections.get(1).getName()).isEqualTo(dataset2.getName());

    final List<ProjectedReportingDatasetCohort> prdcs = datasetJoinTableNativeDao.getReportingDatasetCohorts();
    assertThat(prdcs).hasSize(1);
    assertThat(prdcs.get(0).getCohortId()).isEqualTo(dbCohort.getCohortId());
  }
}
