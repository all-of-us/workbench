package org.pmiops.workbench.dataset.mapper;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.Dataset;
import org.pmiops.workbench.model.DatasetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DatasetMapperTest {

  private DbDataset dbDataset;
  private DbDataDictionaryEntry dbDataDictionaryEntry;

  @Autowired private DatasetMapper datasetMapper;
  @Autowired private ConceptSetDao mockConceptSetDao;
  @Autowired private CohortDao mockCohortDao;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({
    DatasetMapperImpl.class,
    CommonMappers.class,
    ConceptSetService.class,
    ConceptSetMapperImpl.class,
    CohortService.class,
    CohortMapperImpl.class
  })
  @MockBean({
    Clock.class,
    ConceptSetDao.class,
    ConceptBigQueryService.class,
    CohortBuilderService.class,
    ConceptService.class,
    CohortDao.class,
    UserDao.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    dbDataset = new DbDataset();
    dbDataset.setVersion(1);
    dbDataset.setDatasetId(101L);
    dbDataset.setName("All Blue-eyed Blondes");
    dbDataset.setIncludesAllParticipants(false);
    dbDataset.setDescription("All Blue-eyed Blondes");
    dbDataset.setLastModifiedTime(Timestamp.from(Instant.now()));
    dbDataset.setInvalid(false);
    dbDataset.setWorkspaceId(1L);
    dbDataset.setPrePackagedConceptSet(
        DbStorageEnums.prePackagedConceptSetsToStorage(PrePackagedConceptSetEnum.NONE));
    dbDataset.setCohortIds(ImmutableList.of(1L));
    dbDataset.setConceptSetIds(ImmutableList.of(1L));
    dbDataset.setValues(
        ImmutableList.of(
            new DbDatasetValue(
                DbStorageEnums.domainToStorage(Domain.CONDITION).toString(), "value")));
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setConceptSetId(1L);
    DbCohort dbCohort = new DbCohort();
    dbCohort.setCohortId(1L);

    doReturn(Collections.singletonList(dbConceptSet))
        .when(mockConceptSetDao)
        .findAll(dbDataset.getConceptSetIds());
    doReturn(Collections.singletonList(dbCohort))
        .when(mockCohortDao)
        .findAll(dbDataset.getCohortIds());

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    dbDataDictionaryEntry = new DbDataDictionaryEntry();
    dbDataDictionaryEntry.setCdrVersion(cdrVersion);
    dbDataDictionaryEntry.setDefinedTime(Timestamp.from(Instant.now()));
    dbDataDictionaryEntry.setDataProvenance("p");
    dbDataDictionaryEntry.setRelevantOmopTable("person");
    dbDataDictionaryEntry.setFieldName("field");
    dbDataDictionaryEntry.setOmopCdmStandardOrCustomField("field");
    dbDataDictionaryEntry.setDescription("desc");
    dbDataDictionaryEntry.setFieldType("type");
    dbDataDictionaryEntry.setSourcePpiModule("s");
    dbDataDictionaryEntry.setTransformedByRegisteredTierPrivacyMethods(false);
  }

  @Test
  public void dbModelToClientLight() {
    final Dataset toClientDataset = datasetMapper.dbModelToClientLight(dbDataset);
    assertDbModelToClientLight(toClientDataset, dbDataset);
  }

  @Test
  public void dbModelToClientDataset() {
    final Dataset toClientDataset = datasetMapper.dbModelToClient(dbDataset);
    assertDbModelToClient(toClientDataset, dbDataset);
  }

  @Test
  public void dbModelToClientDataDictionaryEntry() {
    final DataDictionaryEntry toClientDataDictionaryEntry =
        datasetMapper.dbModelToClient(dbDataDictionaryEntry);
    assertDbModelToClient(toClientDataDictionaryEntry, dbDataDictionaryEntry);
  }

  @Test
  public void testDatasetRequestToDb() {
    DatasetRequest request = new DatasetRequest();
    request.setName("New Name");
    request.setPrePackagedConceptSet(PrePackagedConceptSetEnum.SURVEY);
    final DbDataset toDataset = datasetMapper.datasetRequestToDb(request, dbDataset, CLOCK);
    assertThat(toDataset.getName()).isEqualTo("New Name");
    assertThat(toDataset.getCohortIds()).isEqualTo(dbDataset.getCohortIds());
    assertThat(toDataset.getIncludesAllParticipants())
        .isEqualTo(dbDataset.getIncludesAllParticipants());
    assertThat(toDataset.getConceptSetIds()).isEqualTo(dbDataset.getConceptSetIds());
    assertThat(toDataset.getValues()).isEqualTo(dbDataset.getValues());
    assertThat(toDataset.getPrePackagedConceptSet()).isEqualTo((short) 0);
    assertThat(toDataset.getInvalid()).isEqualTo(dbDataset.getInvalid());
    assertThat(toDataset.getDatasetId()).isEqualTo(dbDataset.getDatasetId());
    assertThat(toDataset.getCreationTime()).isEqualTo(dbDataset.getCreationTime());
    assertThat(toDataset.getCreatorId()).isEqualTo(dbDataset.getCreatorId());
  }

  @Test
  public void testDatasetRequestToDbWithNullSourceDB() {
    List<Long> conceptIds = ImmutableList.of(4l, 5l, 6l);
    List<Long> cohortIds = ImmutableList.of(1l, 2l, 3l);

    DomainValuePair domainValuePair = new DomainValuePair();
    domainValuePair.setDomain(Domain.PERSON);
    domainValuePair.setValue("person_id");

    DatasetRequest request = new DatasetRequest();
    request.setName("New Name");
    request.setCohortIds(cohortIds);
    request.setConceptSetIds(conceptIds);
    request.setIncludesAllParticipants(false);
    request.setPrePackagedConceptSet(PrePackagedConceptSetEnum.NONE);
    request.setDomainValuePairs(ImmutableList.of(domainValuePair));
    final DbDataset toDataset = datasetMapper.datasetRequestToDb(request, dbDataset, CLOCK);
    assertThat(toDataset.getName()).isEqualTo("New Name");
    assertThat(toDataset.getCohortIds()).isEqualTo(cohortIds);
    assertThat(toDataset.getIncludesAllParticipants()).isFalse();
    assertThat(toDataset.getConceptSetIds()).isEqualTo(conceptIds);
    assertThat(toDataset.getPrePackagedConceptSet()).isEqualTo((short) 0);
    assertThat(toDataset.getValues().get(0).getDomainEnum()).isEqualTo(Domain.PERSON);
    assertThat(toDataset.getInvalid()).isFalse();
  }

  private void assertDbModelToClient(Dataset dataset, DbDataset dbDataset) {
    assertThat(dbDataset.getCohortIds())
        .isEqualTo(dataset.getCohorts().stream().map(Cohort::getId).collect(Collectors.toList()));
    assertThat(dbDataset.getConceptSetIds())
        .isEqualTo(
            dataset.getConceptSets().stream().map(ConceptSet::getId).collect(Collectors.toList()));
    assertThat(dbDataset.getValues())
        .isEqualTo(
            dataset.getDomainValuePairs().stream()
                .map(
                    dvp ->
                        new DbDatasetValue(
                            DbStorageEnums.domainToStorage(dvp.getDomain()).toString(),
                            dvp.getValue()))
                .collect(Collectors.toList()));
    assertDbModelToClientLight(dataset, dbDataset);
  }

  private void assertDbModelToClientLight(Dataset dataset, DbDataset dbDataset) {
    assertThat(dbDataset.getDatasetId()).isEqualTo(dataset.getId());
    assertThat(dbDataset.getVersion()).isEqualTo(Etags.toVersion(dataset.getEtag()));
    assertThat(dbDataset.getName()).isEqualTo(dataset.getName());
    assertThat(dbDataset.getIncludesAllParticipants())
        .isEqualTo(dataset.getIncludesAllParticipants());
    assertThat(dbDataset.getDescription()).isEqualTo(dataset.getDescription());
    assertThat(dbDataset.getWorkspaceId()).isEqualTo(dataset.getWorkspaceId());
    assertThat(dbDataset.getLastModifiedTime().toInstant().toEpochMilli())
        .isEqualTo(dataset.getLastModifiedTime());
    assertThat(dbDataset.getPrePackagedConceptSet())
        .isEqualTo(
            DbStorageEnums.prePackagedConceptSetsToStorage(dataset.getPrePackagedConceptSet()));
  }

  private void assertDbModelToClient(
      DataDictionaryEntry dataDictionaryEntry, DbDataDictionaryEntry dbDataDictionaryEntry) {
    assertThat(dbDataDictionaryEntry.getCdrVersion().getCdrVersionId())
        .isEqualTo(dataDictionaryEntry.getCdrVersionId());
    assertThat(dbDataDictionaryEntry.getDefinedTime().toInstant().toEpochMilli())
        .isEqualTo(dataDictionaryEntry.getDefinedTime());
    assertThat(dbDataDictionaryEntry.getDataProvenance())
        .isEqualTo(dataDictionaryEntry.getDataProvenance());
    assertThat(dbDataDictionaryEntry.getRelevantOmopTable())
        .isEqualTo(dataDictionaryEntry.getRelevantOmopTable());
    assertThat(dbDataDictionaryEntry.getDescription())
        .isEqualTo(dataDictionaryEntry.getDescription());
    assertThat(dbDataDictionaryEntry.getFieldName()).isEqualTo(dataDictionaryEntry.getFieldName());
    assertThat(dbDataDictionaryEntry.getOmopCdmStandardOrCustomField())
        .isEqualTo(dataDictionaryEntry.getOmopCdmStandardOrCustomField());
    assertThat(dbDataDictionaryEntry.getDescription())
        .isEqualTo(dataDictionaryEntry.getDescription());
    assertThat(dbDataDictionaryEntry.getFieldType()).isEqualTo(dataDictionaryEntry.getFieldType());
    assertThat(dbDataDictionaryEntry.getSourcePpiModule())
        .isEqualTo(dataDictionaryEntry.getSourcePpiModule());
    assertThat(dbDataDictionaryEntry.getTransformedByRegisteredTierPrivacyMethods())
        .isEqualTo(dataDictionaryEntry.getTransformedByRegisteredTierPrivacyMethods());
  }
}
