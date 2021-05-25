package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@ExtendWith(SpringExtension.class)
public class CohortReviewMapperTest {

  @Autowired private CohortReviewMapper cohortReviewMapper;

  @TestConfiguration
  @Import({CohortReviewMapperImpl.class, CommonMappers.class})
  @MockBean({Clock.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    CohortReview expectedCohortReview =
        new CohortReview()
            .cohortReviewId(1L)
            .cdrVersionId(1L)
            .cohortDefinition("def")
            .cohortId(1L)
            .cohortName("name")
            .cohortReviewId(1L)
            .creationTime(timestamp.getTime())
            .description("descr")
            .etag(Etags.fromVersion(1))
            .lastModifiedTime(timestamp.getTime())
            .matchedParticipantCount(200L)
            .reviewSize(10L)
            .reviewStatus(ReviewStatus.CREATED)
            .reviewedCount(10L);
    assertThat(
            cohortReviewMapper.dbModelToClient(
                new DbCohortReview()
                    .cohortReviewId(1L)
                    .cohortDefinition("def")
                    .cohortId(1L)
                    .cohortName("name")
                    .version(1)
                    .reviewSize(10L)
                    .reviewStatus(new Short("1"))
                    .reviewedCount(10L)
                    .cdrVersionId(1L)
                    .creationTime(timestamp)
                    .lastModifiedTime(timestamp)
                    .description("descr")
                    .matchedParticipantCount(200)))
        .isEqualTo(expectedCohortReview);
  }
}
