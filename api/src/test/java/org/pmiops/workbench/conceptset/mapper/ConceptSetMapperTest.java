package org.pmiops.workbench.conceptset.mapper;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetConceptId;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;


public class ConceptSetMapperTest {

  @Autowired private ConceptSetMapper conceptSetMapper;
  @Autowired private ConceptBigQueryService conceptBigQueryService;

  private DbConceptSet dbConceptSet;

  @TestConfiguration
  @Import({ConceptSetMapperImpl.class, CommonMappers.class})
  @MockBean({ConceptBigQueryService.class, Clock.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    DbUser creator = new DbUser();
    creator.setUsername("brian");
    dbConceptSet =
        new DbConceptSet(
            "conceptSet",
            1,
            DbStorageEnums.domainToStorage(Domain.CONDITION),
            DbStorageEnums.surveysToStorage(Surveys.THE_BASICS),
            "descr",
            1,
            creator,
            now,
            now);
    dbConceptSet.setConceptSetId(1);
  }

  @Test
  public void dbModelToClient() {
    ConceptSet clientConceptSet = conceptSetMapper.dbModelToClient(dbConceptSet);
    assertThat(clientConceptSet.getId()).isEqualTo(dbConceptSet.getConceptSetId());
    assertThat(Etags.toVersion(clientConceptSet.getEtag())).isEqualTo(dbConceptSet.getVersion());
    assertThat(clientConceptSet.getDomain()).isEqualTo(dbConceptSet.getDomainEnum());
    assertThat(clientConceptSet.getSurvey()).isEqualTo(dbConceptSet.getSurveysEnum());
    assertThat(clientConceptSet.getName()).isEqualTo(dbConceptSet.getName());
    assertThat(clientConceptSet.getDescription()).isEqualTo(dbConceptSet.getDescription());
    assertThat(clientConceptSet.getCreationTime())
        .isEqualTo(dbConceptSet.getCreationTime().getTime());
    assertThat(clientConceptSet.getLastModifiedTime())
        .isEqualTo(dbConceptSet.getLastModifiedTime().getTime());
    assertThat(clientConceptSet.getCreator()).isEqualTo(dbConceptSet.getCreator().getUsername());
    assertThat(clientConceptSet.getCriteriums()).isNull();
  }

  @Test
  public void clientToDbModel() {
    ConceptSet clientConceptSet = conceptSetMapper.dbModelToClient(dbConceptSet);
    ConceptSetConceptId conceptSetConceptId1 = new ConceptSetConceptId();
    conceptSetConceptId1.setConceptId(1L);
    conceptSetConceptId1.setStandard(true);
    ConceptSetConceptId conceptSetConceptId2 = new ConceptSetConceptId();
    conceptSetConceptId2.setConceptId(2L);
    conceptSetConceptId2.setStandard(true);
    ConceptSetConceptId conceptSetConceptId3 = new ConceptSetConceptId();
    conceptSetConceptId3.setConceptId(3L);
    conceptSetConceptId3.setStandard(true);
    CreateConceptSetRequest conceptSetRequest = new CreateConceptSetRequest();
    conceptSetRequest.setConceptSet(clientConceptSet);
    conceptSetRequest.setAddedConceptSetConceptIds(
        Arrays.asList(conceptSetConceptId1, conceptSetConceptId2, conceptSetConceptId3));
    conceptSetMapper.clientToDbModel(conceptSetRequest, 1l, dbConceptSet.getCreator());
    assertThat(clientConceptSet.getId()).isEqualTo(dbConceptSet.getConceptSetId());
    assertThat(Etags.toVersion(clientConceptSet.getEtag())).isEqualTo(dbConceptSet.getVersion());
    assertThat(clientConceptSet.getDomain()).isEqualTo(dbConceptSet.getDomainEnum());
    assertThat(clientConceptSet.getSurvey()).isEqualTo(dbConceptSet.getSurveysEnum());
    assertThat(clientConceptSet.getName()).isEqualTo(dbConceptSet.getName());
    assertThat(clientConceptSet.getDescription()).isEqualTo(dbConceptSet.getDescription());
    assertThat(clientConceptSet.getCreationTime())
        .isEqualTo(dbConceptSet.getCreationTime().getTime());
    assertThat(clientConceptSet.getLastModifiedTime())
        .isEqualTo(dbConceptSet.getLastModifiedTime().getTime());
    assertThat(clientConceptSet.getCreator()).isEqualTo(dbConceptSet.getCreator().getUsername());
    assertThat(clientConceptSet.getCriteriums()).isNull();
  }
}
