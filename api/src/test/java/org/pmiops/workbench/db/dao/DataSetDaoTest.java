package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.PrePackagedConceptSetSelection;
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

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private DataSetDao dataSetDao;

  @Before
  public void setup() {
    // FK constraint requires a real workspaceID
    DbWorkspace workspace = new DbWorkspace();
    workspace = workspaceDao.save(workspace);
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(true, workspace.getWorkspaceId());
    insertDatasetForGauge(false, workspace.getWorkspaceId());
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
    dataset.setPrePackagedConceptSetEnum(PrePackagedConceptSetSelection.BOTH);
    dataset.setWorkspaceId(workspaceId);
    return dataSetDao.save(dataset);
  }

  @Test
  public void test() {
    final Map<Boolean, Long> map = dataSetDao.getInvalidToCountMap();
    assertThat(map).hasSize(Booleans.VALUE_STRINGS.size());
    assertThat(map.get(true)).isEqualTo(2L);
    assertThat(map.get(false)).isEqualTo(1L);
  }
}
