package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class GoogleProjectPerCostDaoTest {

  @Autowired private GoogleProjectPerCostDao googleProjectPerCostDao;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Import({GoogleProjectPerCostDao.class, FakeClockConfiguration.class})
  @TestConfiguration
  static class Configuration {}

  @Test
  public void testInsertGetDeleteAll() {
    DbGoogleProjectPerCost dbGoogleProjectPerCost1 =
        new DbGoogleProjectPerCost().setGoogleProjectId("Project1").setCost(1.0);
    DbGoogleProjectPerCost dbGoogleProjectPerCost2 =
        new DbGoogleProjectPerCost().setGoogleProjectId("Project2").setCost(2.0);
    DbGoogleProjectPerCost dbGoogleProjectPerCost3 =
        new DbGoogleProjectPerCost().setGoogleProjectId("Project3").setCost(3.0);

    googleProjectPerCostDao.batchInsertProjectPerCost(
        Arrays.asList(dbGoogleProjectPerCost1, dbGoogleProjectPerCost2, dbGoogleProjectPerCost3));

    System.out.println(googleProjectPerCostDao.findAll().iterator().next().getGoogleProjectId());
    List<DbGoogleProjectPerCost> project1List =
        (List<DbGoogleProjectPerCost>)
            googleProjectPerCostDao.findAllByGoogleProjectId(
                Sets.newHashSet("Project1", "Project2"));

    assertThat(project1List).containsExactly(dbGoogleProjectPerCost1, dbGoogleProjectPerCost2);

    googleProjectPerCostDao.deleteAll();
    assertThat(
            googleProjectPerCostDao.findAllByGoogleProjectId(
                Sets.newHashSet("Project1", "Project2", "Project3")))
        .isEmpty();
  }
}
