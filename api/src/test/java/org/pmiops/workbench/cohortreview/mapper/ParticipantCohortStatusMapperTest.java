package org.pmiops.workbench.cohortreview.mapper;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.sql.Date;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class ParticipantCohortStatusMapperTest {

  @Autowired private ParticipantCohortStatusMapper participantCohortStatusMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    ParticipantCohortStatusMapperImpl.class,
    CommonMappers.class
  })
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
            .gender("Man");
    Table<Long, CriteriaType, String> demoTable = HashBasedTable.create();
    demoTable.put(1L, CriteriaType.ETHNICITY, "Latino");
    demoTable.put(2L, CriteriaType.RACE, "White");
    demoTable.put(3L, CriteriaType.SEX, "Male");
    demoTable.put(4L, CriteriaType.GENDER, "Man");

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
                demoTable))
        .isEqualTo(expectedParticipantCohortStatus);
  }
}
