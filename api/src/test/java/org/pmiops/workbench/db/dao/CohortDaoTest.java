package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CohortDaoTest {

  @Autowired CohortDao cohortDao;
  @Autowired WorkspaceDao workspaceDao;
  private DbCohort dbCohort;

  @Before
  public void setUp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setName("name");
    dbWorkspace.setWorkspaceNamespace("name");
    dbWorkspace.setFirecloudName("name");
    dbWorkspace.setDataAccessLevel((short) 1);
    dbWorkspace.setCreationTime(timestamp);
    dbWorkspace.setLastModifiedTime(timestamp);
    workspaceDao.save(dbWorkspace);

    String cohortJson =
        "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":"
            + "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":"
            + "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";
    dbCohort = new DbCohort();
    dbCohort.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort.setName("name");
    dbCohort.setCriteria(cohortJson);
    dbCohort = cohortDao.save(dbCohort);
  }

  @Test
  public void findCohortByCohortId() {
    assertThat(cohortDao.findAllByCohortIdIn(ImmutableList.of(dbCohort.getCohortId())).get(0))
        .isEqualTo(dbCohort);
  }

  @Test
  public void findCohortByNameAndWorkspaceId() {
    assertThat(
            cohortDao.findCohortByNameAndWorkspaceId(dbCohort.getName(), dbCohort.getWorkspaceId()))
        .isEqualTo(dbCohort);
  }

  @Test
  public void findByWorkspaceId() {
    assertThat(cohortDao.findByWorkspaceId(dbCohort.getWorkspaceId()).get(0)).isEqualTo(dbCohort);
  }
}
