package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CohortMapperTest {

  private Cohort sourceClientCohort;
  private DbCohort sourceDbCohort;

  @Autowired private CohortMapper cohortMapper;
  @Autowired private UserDao mockUserDao;

  @TestConfiguration
  @Import({CohortMapperImpl.class})
  @MockBean({UserDao.class})
  static class Configuration {}

  @Before
  public void setUp() {
    sourceClientCohort =
        new Cohort()
            .id(101L)
            .etag("ETAG_ETAG")
            .name("All Blue-eyed Blondes")
            .criteria("blue eyes and blonde hair")
            .type("Demographics")
            .description("A cohort I found the other day.")
            .creator("billg@msn.com")
            .creationTime(Instant.parse("2018-01-01T23:59:59.00Z").toEpochMilli())
            .lastModifiedTime(Instant.parse("2019-01-01T23:59:59.00Z").toEpochMilli());

    // right now we're using same creator for all tests
    final DbUser creator = new DbUser();
    creator.setUserName("billg@msn.com");
    creator.setContactEmail("bill@terra.bio");
    creator.setUserId(888L);

    doReturn(creator).when(mockUserDao).findUserByEmail(creator.getUserName());

    final DbCohortReview review = new DbCohortReview();
    review.setCdrVersionId(3);
    review.setCohortId(202L);

    final ImmutableSet<DbCohortReview> cohortReviews = ImmutableSet.of(review);

    sourceDbCohort = new DbCohort();
    sourceDbCohort.setCohortId(202L);
    sourceDbCohort.setVersion(3);
    sourceDbCohort.setName("shorter_people");
    sourceDbCohort.setType("conditions");
    sourceDbCohort.setDescription("People who are shorter than 5'9\"");
    sourceDbCohort.setWorkspaceId(999L);
    sourceDbCohort.setCriteria("h < 1.3");
    sourceDbCohort.setCreator(creator);
    sourceDbCohort.setCreationTime(Timestamp.from(Instant.parse("2011-01-03T23:59:59.00Z")));
    sourceDbCohort.setLastModifiedTime(Timestamp.from(Instant.parse("2018-05-01T23:59:59.00Z")));
    sourceDbCohort.setCohortReviews(cohortReviews);
  }

  @Test
  public void testConvertsClientCohortToDbCohort() {
    final DbCohort dbModel = cohortMapper.clientToDbModel(sourceClientCohort);
    assertCorrespondingFieldsMatch(sourceClientCohort, dbModel);

    final Cohort roundTrippedCohort = cohortMapper.dbModelToClient(dbModel);
    assertCorrespondingFieldsMatch(roundTrippedCohort, dbModel);
  }

  @Test
  public void testConvertsDbCohortToClientCohort() {
    final Cohort clientCohort = cohortMapper.dbModelToClient(sourceDbCohort);
    assertCorrespondingFieldsMatch(clientCohort, sourceDbCohort);

    final DbCohort roundTrippedDbCohort = cohortMapper.clientToDbModel(clientCohort);
    assertCorrespondingFieldsMatch(clientCohort, roundTrippedDbCohort);
  }

  private void assertCorrespondingFieldsMatch(Cohort clientCohort, DbCohort dbModelCohort) {
    assertThat(dbModelCohort.getCohortId()).isEqualTo(clientCohort.getId());
    assertThat(dbModelCohort.getName()).isEqualTo(clientCohort.getName());
    assertThat(dbModelCohort.getCriteria()).isEqualTo(clientCohort.getCriteria());
    assertThat(dbModelCohort.getType()).isEqualTo(clientCohort.getType());
    assertThat(dbModelCohort.getDescription()).isEqualTo(clientCohort.getDescription());
    assertThat(dbModelCohort.getCreator().getUserName()).isEqualTo(clientCohort.getCreator());
    assertThat(dbModelCohort.getCreationTime().toInstant().toEpochMilli())
        .isEqualTo(clientCohort.getCreationTime());
    assertThat(dbModelCohort.getLastModifiedTime().toInstant().toEpochMilli())
        .isEqualTo(clientCohort.getLastModifiedTime());
  }
}
