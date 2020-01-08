package org.pmiops.workbench.db.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbCohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CohortDaoTest {

  public static final long WORKSPACE_ID = 9999;
  @Autowired CohortDao cohortDao;

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  public void findCohortByCohortId() {
    String cohortJson =
        "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":"
            + "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":"
            + "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";

    DbCohort cohort = new DbCohort();
    cohort.setWorkspaceId(WORKSPACE_ID);
    cohort.setCriteria(cohortJson);

    // need to insert a workspace to satisfy the foreign key contraint of cohort
    jdbcTemplate.execute(
        "insert into workspace"
            + "(workspace_id, name, workspace_namespace, firecloud_name, data_access_level, creation_time, last_modified_time)"
            + "values ("
            + WORKSPACE_ID
            + ", 'name', 'name', 'name', 1, sysdate(), sysdate())");

    cohortDao.save(cohort);

    assertEquals(cohortJson, cohort.getCriteria());
  }
}
