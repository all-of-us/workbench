package org.pmiops.workbench.db.dao;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortDefinition;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CohortDaoTest {

    public static final long WORKSPACE_ID = 9999;
    @Autowired
    CohortDao cohortDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void findCohortByCohortId() throws Exception {
        String cohortJson = "{\"includes\":[{\"items\":[{\"type\":\"DEMO\",\"searchParameters\":" +
                "[{\"value\":\"Age\",\"subtype\":\"AGE\",\"conceptId\":null,\"attribute\":" +
                "{\"operator\":\"between\",\"operands\":[18,66]}}],\"modifiers\":[]}]}],\"excludes\":[]}";

        Cohort cohort = new Cohort();
        cohort.setWorkspaceId(WORKSPACE_ID);
        cohort.setCriteria(cohortJson);

        //need to insert a workspace to satisfy the foreign key contraint of cohort
        jdbcTemplate.execute("insert into workspace" +
                "(workspace_id, name, workspace_namespace, firecloud_name, data_access_level, creation_time, last_modified_time)" +
                "values (" + WORKSPACE_ID + ", 'name', 'name', 'name', 1, sysdate(), sysdate())");

        cohortDao.save(cohort);

        assertEquals(cohortJson, cohort.getCriteria());
    }
}
