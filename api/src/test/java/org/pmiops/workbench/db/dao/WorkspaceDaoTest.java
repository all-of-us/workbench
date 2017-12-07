package org.pmiops.workbench.db.dao;

import static org.springframework.test.util.AssertionErrors.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspaceDaoTest {

  public static final long WORKSPACE_ID = 9999;

  @Autowired
  WorkspaceDao workspaceDao;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Test
  public void testWorkspaceVersionLocking() {
    org.pmiops.workbench.db.model.Workspace ws = new org.pmiops.workbench.db.model.Workspace();
    ws.setVersion(1);
    ws = workspaceDao.save(ws);

    // Version incremented to 2.
    ws.setName("foo");
    ws = workspaceDao.save(ws);

    try {
      ws.setName("bar");
      ws.setVersion(1);
      workspaceDao.save(ws);
      fail("expected optimistic lock exception on stale version update");
    } catch(ObjectOptimisticLockingFailureException e) {
      // expected
    }
  }

  @Test
  public void findOne() throws Exception {

    jdbcTemplate.execute("insert into user " +
            "(user_id, email, data_access_level, given_name, family_name, free_tier_billing_project_name, first_sign_in_time, version) " +
            "values (1, 'brian.freeman@fake-research-aou.org', 0, 'Brian Freeman', 'Brian Freeman', 'aou-test-free-743820425', sysdate(), 0)");

    jdbcTemplate.execute("insert into cdr_version " +
            "(cdr_version_id, name, release_number, data_access_level, bigquery_project, bigquery_dataset, num_participants) " +
            "values (1, 'version 1', '1', 1, '1', '1', 1000000)");

    jdbcTemplate.execute("insert into workspace " +
            "(workspace_id, name, workspace_namespace, firecloud_name, data_access_level, cdr_version_id, " +
            "creation_time, last_modified_time, rp_aggregate_analysis, rp_ancestry, rp_commercial_purpose, " +
            "rp_population, rp_control_set, rp_methods_development, rp_disease_focused_research, version, creator_id) " +
            "values (" + WORKSPACE_ID + ", 'name', 'name', 'name', 1, 1, sysdate(), sysdate(), 0, 0, 0, 0, 0, 0, 0, 1, 1)");

//    jdbcTemplate.execute("insert into user_workspace " +
//            "(user_id, workspace_id, role) " +
//            "values (1, WORKSPACE_ID, 3)");

    Workspace workspace = workspaceDao.findOne(WORKSPACE_ID);
    System.out.println(workspace.toString());
  }
}
