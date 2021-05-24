package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Date;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ParticipantCohortAnnotationMapperTest {

  @Autowired private ParticipantCohortAnnotationMapper participantCohortAnnotationMapper;

  @TestConfiguration
  @Import({ParticipantCohortAnnotationMapperImpl.class, CommonMappers.class})
  @MockBean({Clock.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    Date today = new Date(System.currentTimeMillis());
    ParticipantCohortAnnotation participantCohortAnnotation =
        new ParticipantCohortAnnotation()
            .cohortAnnotationDefinitionId(1L)
            .cohortReviewId(1L)
            .participantId(1L)
            .annotationValueDate(today.toString())
            .annotationId(1L);
    assertThat(
            participantCohortAnnotationMapper.dbModelToClient(
                new DbParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(1L)
                    .cohortReviewId(1L)
                    .participantId(1L)
                    .annotationValueDate(today)
                    .annotationId(1L)))
        .isEqualTo(participantCohortAnnotation);
  }

  @Test
  public void clientToDbModel() {
    Date today = new Date(System.currentTimeMillis());
    DbParticipantCohortAnnotation dbParticipantCohortAnnotation =
        new DbParticipantCohortAnnotation()
            .cohortAnnotationDefinitionId(1L)
            .cohortReviewId(1L)
            .participantId(1L)
            .annotationValueDateString(today.toString())
            .annotationId(1L);
    assertThat(
            participantCohortAnnotationMapper.clientToDbModel(
                new ParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(1L)
                    .cohortReviewId(1L)
                    .participantId(1L)
                    .annotationValueDate(today.toString())
                    .annotationId(1L)))
        .isEqualTo(dbParticipantCohortAnnotation);
  }
}
