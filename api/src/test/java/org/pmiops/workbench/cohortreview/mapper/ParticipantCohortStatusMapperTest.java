package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import java.sql.Date;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class ParticipantCohortStatusMapperTest {

  @Autowired private ParticipantCohortStatusMapper participantCohortStatusMapper;

  @TestConfiguration
  @Import({ParticipantCohortStatusMapperImpl.class, CommonMappers.class})
  @MockBean({Clock.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    Date birthDate = new Date(System.currentTimeMillis());

    ParticipantCohortStatus expectedParticipantCohortStatus =
        new ParticipantCohortStatus()
            .status(CohortStatus.INCLUDED)
            .participantId(1L)
            .birthDate(birthDate.toString())
            .deceased(false)
            .ethnicityConceptId(1L)
            .ethnicity("Latino")
            .raceConceptId(2L)
            .race("White")
            .sexAtBirthConceptId(3L)
            .sexAtBirth("Male")
            .genderConceptId(4L)
            .gender("Male");
    assertThat(
            participantCohortStatusMapper.dbModelToClient(
                new DbParticipantCohortStatus()
                    .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.INCLUDED))
                    .participantKey(
                        new DbParticipantCohortStatusKey().participantId(1L).cohortReviewId(1L))
                    .birthDate(birthDate)
                    .deceased(false)
                    .ethnicityConceptId(1L)
                    .raceConceptId(2L)
                    .sexAtBirthConceptId(3L)
                    .genderConceptId(4L),
                ImmutableMap.of(
                    1L, "Latino",
                    2L, "White",
                    3L, "Male",
                    4L, "Male")))
        .isEqualTo(expectedParticipantCohortStatus);
  }
}
