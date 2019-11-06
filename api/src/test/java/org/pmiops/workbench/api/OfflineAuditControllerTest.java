package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class OfflineAuditControllerTest {
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String CDR_V1_PROJECT_ID = "cdr1-project";
  private static final String CDR_V2_PROJECT_ID = "cdr2-project";
  private static final String FC_PROJECT_ID = "fc-project";
  private static final String USER_EMAIL = "falco@lombardi.com";

  @TestConfiguration
  @Import({OfflineAuditController.class})
  @MockBean({BigQueryService.class, WorkspaceDao.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Autowired BigQueryService bigQueryService;
  @Autowired UserDao userDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired OfflineAuditController offlineAuditController;
  @Autowired WorkspaceDao workspaceDao;

  @Before
  public void setUp() {
    DbUser user = new DbUser();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user = userDao.save(user);

    CdrVersion cdrV1 = new CdrVersion();
    cdrV1.setBigqueryProject(CDR_V1_PROJECT_ID);
    cdrV1 = cdrVersionDao.save(cdrV1);
    CdrVersion cdrV2 = new CdrVersion();
    cdrV2.setBigqueryProject(CDR_V2_PROJECT_ID);
    cdrV2 = cdrVersionDao.save(cdrV2);

    when(workspaceDao.findAllWorkspaceNamespaces()).thenReturn(ImmutableSet.of(FC_PROJECT_ID));

    CLOCK.setInstant(NOW);
  }

  // TODO(RW-350): This stubbing is awful, improve this.
  private void stubBigQueryCalls(String projectId, String email, long total) {
    TableResult queryResult = mock(TableResult.class);
    Iterable testIterable =
        new Iterable() {
          @Override
          public Iterator iterator() {
            List<FieldValue> list = new ArrayList<>();
            list.add(null);
            return list.iterator();
          }
        };
    Map<String, Integer> rm =
        ImmutableMap.<String, Integer>builder()
            .put("client_project_id", 0)
            .put("user_email", 1)
            .put("total", 2)
            .build();

    when(bigQueryService.executeQuery(any())).thenReturn(queryResult);
    when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
    when(queryResult.iterateAll()).thenReturn(testIterable);
    when(bigQueryService.getString(null, 0)).thenReturn(projectId);
    when(bigQueryService.getString(null, 1)).thenReturn(email);
    when(bigQueryService.getLong(null, 2)).thenReturn(total);
  }

  @Test
  public void testAuditTableSuffix() {
    assertThat(OfflineAuditController.auditTableSuffix(Instant.parse("2007-01-03T00:00:00.00Z"), 0))
        .isEqualTo("20070103");
    assertThat(OfflineAuditController.auditTableSuffix(Instant.parse("2018-01-01T23:59:59.00Z"), 3))
        .isEqualTo("20171229");
  }

  @Test
  public void testAuditBigQueryCdrV1Queries() {
    stubBigQueryCalls(CDR_V1_PROJECT_ID, USER_EMAIL, 5);
    assertThat(offlineAuditController.auditBigQuery().getBody().getNumQueryIssues()).isEqualTo(0);
  }

  @Test
  public void testAuditBigQueryCdrV2Queries() {
    stubBigQueryCalls(CDR_V2_PROJECT_ID, USER_EMAIL, 5);
    assertThat(offlineAuditController.auditBigQuery().getBody().getNumQueryIssues()).isEqualTo(0);
  }

  @Test
  public void testAuditBigQueryFirecloudQueries() {
    stubBigQueryCalls(FC_PROJECT_ID, USER_EMAIL, 5);
    assertThat(offlineAuditController.auditBigQuery().getBody().getNumQueryIssues()).isEqualTo(0);
  }

  @Test
  public void testAuditBigQueryUnrecognizedProjectQueries() {
    stubBigQueryCalls("my-personal-gcp-project", USER_EMAIL, 5);
    // These stubs are hit once per CDR project, so the total number of issues is doubled.
    assertThat(offlineAuditController.auditBigQuery().getBody().getNumQueryIssues()).isEqualTo(10);
  }
}
