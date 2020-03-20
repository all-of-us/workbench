package org.pmiops.workbench.conceptset;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ConceptSetMapperTest {

  @Autowired private ConceptSetMapper conceptSetMapper;

  private DbConceptSet dbConceptSet;

  @TestConfiguration
  @Import({
      ConceptSetMapperImpl.class,
      CommonMappers.class
  })
  static class Configuration {}

  @Before
  public void setUp() {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    DbUser creator = new DbUser();
    creator.setUsername("brian");
    dbConceptSet =
        DbConceptSet.builder()
            .addConceptSetId(1)
            .addCreationTime(now)
            .addCreator(creator)
            .addDescription("descr")
            .addDomain(DbStorageEnums.domainToStorage(Domain.CONDITION))
            .addLastModifiedTime(now)
            .addName("conceptSet")
            .addParticipantCount(200)
            .addSurvey(DbStorageEnums.surveysToStorage(Surveys.THE_BASICS))
            .addVersion(1)
            .addWorkspaceId(1)
            .build();
  }

  @Test
  public void dbModelToClient() {
    ConceptSet clientConceptSet = conceptSetMapper.dbModelToClient(dbConceptSet);
    assertThat(clientConceptSet.getId()).isEqualTo(dbConceptSet.getConceptSetId());
    assertThat(Etags.toVersion(clientConceptSet.getEtag())).isEqualTo(dbConceptSet.getVersion());
    assertThat(clientConceptSet.getDomain()).isEqualTo(dbConceptSet.getDomainEnum());
    assertThat(clientConceptSet.getSurvey()).isEqualTo(dbConceptSet.getSurveysEnum());
    assertThat(clientConceptSet.getName()).isEqualTo(dbConceptSet.getName());
    assertThat(clientConceptSet.getParticipantCount())
        .isEqualTo(dbConceptSet.getParticipantCount());
    assertThat(clientConceptSet.getDescription()).isEqualTo(dbConceptSet.getDescription());
    assertThat(clientConceptSet.getCreationTime())
        .isEqualTo(dbConceptSet.getCreationTime().getTime());
    assertThat(clientConceptSet.getLastModifiedTime())
        .isEqualTo(dbConceptSet.getLastModifiedTime().getTime());
    assertThat(clientConceptSet.getCreator()).isEqualTo(dbConceptSet.getCreator().getUsername());
    assertThat(clientConceptSet.getConcepts()).isNull();
  }
}
