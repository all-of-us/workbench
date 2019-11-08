package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Cohort;

public class CohortMapperTest {

  private Cohort sourceClientCohort;
  private DbCohort sourceDbCohort;
  private final CohortMapper cohortMapper = Mappers.getMapper(CohortMapper.class);;

  @Before
  public void setUp() {
    sourceClientCohort = new Cohort();
    sourceClientCohort.setId(101L);
    sourceClientCohort.setEtag("ETAG_ETAG");
    sourceClientCohort.setName("All Blue-eyed Blondes");
    sourceClientCohort.setCriteria("blue eyes and blonde hair");
    sourceClientCohort.setType("Demographics");
    sourceClientCohort.setDescription("A cohort I found the other day.");
    sourceClientCohort.setCreator("jay@all-of.us");
    sourceClientCohort.setCreationTime(Instant.parse("2018-01-01T23:59:59.00Z").toEpochMilli());
    sourceClientCohort.setLastModifiedTime(Instant.parse("2019-01-01T23:59:59.00Z").toEpochMilli());

    final DbUser creator = new DbUser();
    creator.setEmail("billg@msn.com");

    final DbCohortReview review1 = new DbCohortReview();
    review1.setCdrVersionId(3);
    review1.setCohortId(202L);

    final ImmutableSet<DbCohortReview> cohortReviews = ImmutableSet.of(review1);

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
    assertThat(dbModelCohort.getCreator().getEmail()).isEqualTo(clientCohort.getCreator());
    assertThat(dbModelCohort.getCreationTime().toInstant().toEpochMilli())
        .isEqualTo(clientCohort.getCreationTime());
    assertThat(dbModelCohort.getLastModifiedTime().toInstant().toEpochMilli())
        .isEqualTo(clientCohort.getLastModifiedTime());
  }
}
