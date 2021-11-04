package org.pmiops.workbench.dataset.mapper;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class DataSetMapperTest {

  private DbDataset dbDataset;
  private DbDSDataDictionary dbDataDictionaryEntry;

  @Autowired private DataSetMapper dataSetMapper;
  @Autowired private ConceptSetDao mockConceptSetDao;
  @Autowired private CohortDao mockCohortDao;
  @Autowired private FakeClock fakeClock;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    DataSetMapperImpl.class,
    CommonMappers.class,
    ConceptSetService.class,
    ConceptSetMapperImpl.class,
    CohortService.class,
    CohortMapperImpl.class
  })
  @MockBean({
    BigQueryService.class,
    CloudStorageClient.class,
    ConceptSetDao.class,
    ConceptBigQueryService.class,
    CohortBuilderService.class,
    CohortBuilderMapper.class,
    CohortQueryBuilder.class,
    CohortDao.class,
    FireCloudService.class,
    WgsExtractCromwellSubmissionDao.class,
    UserDao.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    dbDataset = new DbDataset();
    dbDataset.setVersion(1);
    dbDataset.setDataSetId(101L);
    dbDataset.setName("All Blue-eyed Blondes");
    dbDataset.setIncludesAllParticipants(false);
    dbDataset.setDescription("All Blue-eyed Blondes");
    dbDataset.setLastModifiedTime(Timestamp.from(Instant.now()));
    dbDataset.setInvalid(false);
    dbDataset.setWorkspaceId(1L);
    dbDataset.setPrePackagedConceptSet(
        Collections.singletonList(
            DbStorageEnums.prePackagedConceptSetsToStorage(PrePackagedConceptSetEnum.NONE)));
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
        .findAllById(dbDataset.getConceptSetIds());
    doReturn(Collections.singletonList(dbCohort))
        .when(mockCohortDao)
        .findAllById(dbDataset.getCohortIds());

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    dbDataDictionaryEntry = new DbDSDataDictionary();
    dbDataDictionaryEntry.setDataProvenance("p");
    dbDataDictionaryEntry.setRelevantOmopTable("person");
    dbDataDictionaryEntry.setFieldName("field");
    dbDataDictionaryEntry.setOmopCdmStandardOrCustomField("field");
    dbDataDictionaryEntry.setDescription("desc");
    dbDataDictionaryEntry.setFieldType("type");
    dbDataDictionaryEntry.setSourcePpiModule("s");
  }

  @Test
  public void dbModelToClientLight() {
    final DataSet toClientDataSet = dataSetMapper.dbModelToClientLight(dbDataset);
    assertDbModelToClientLight(toClientDataSet, dbDataset);
  }

  @Test
  public void dbModelToClientDataSet() {
    final DataSet toClientDataSet = dataSetMapper.dbModelToClient(dbDataset);
    assertDbModelToClient(toClientDataSet, dbDataset);
  }

  @Test
  public void dbModelToClientDataDictionaryEntry() {
    final DataDictionaryEntry toClientDataDictionaryEntry =
        dataSetMapper.dbDsModelToClient(dbDataDictionaryEntry);
    assertDbModelToClient(toClientDataDictionaryEntry, dbDataDictionaryEntry);
  }

  @Test
  public void testDataSetRequestToDb() {
    DataSetRequest request = new DataSetRequest();
    request.setName("New Name");
    request.setPrePackagedConceptSet(
        Arrays.asList(PrePackagedConceptSetEnum.SURVEY, PrePackagedConceptSetEnum.FITBIT_ACTIVITY));
    final DbDataset toDataSet = dataSetMapper.dataSetRequestToDb(request, dbDataset, fakeClock);
    assertThat(toDataSet.getName()).isEqualTo("New Name");
    assertThat(toDataSet.getCohortIds()).isEqualTo(dbDataset.getCohortIds());
    assertThat(toDataSet.getIncludesAllParticipants())
        .isEqualTo(dbDataset.getIncludesAllParticipants());
    assertThat(toDataSet.getConceptSetIds()).isEqualTo(dbDataset.getConceptSetIds());
    assertThat(toDataSet.getValues()).isEqualTo(dbDataset.getValues());
    assertThat(toDataSet.getPrePackagedConceptSet().size()).isEqualTo(1);
    assertThat(toDataSet.getPrePackagedConceptSet().get(0)).isEqualTo((short) 0);
    assertThat(toDataSet.getInvalid()).isEqualTo(dbDataset.getInvalid());
    assertThat(toDataSet.getDataSetId()).isEqualTo(dbDataset.getDataSetId());
    assertThat(toDataSet.getCreationTime()).isEqualTo(dbDataset.getCreationTime());
    assertThat(toDataSet.getCreatorId()).isEqualTo(dbDataset.getCreatorId());
  }

  @Test
  public void testDataSetRequestToDbWithNullSourceDB() {
    List<Long> conceptIds = ImmutableList.of(4L, 5L, 6L);
    List<Long> cohortIds = ImmutableList.of(1L, 2L, 3L);

    DomainValuePair domainValuePair = new DomainValuePair();
    domainValuePair.setDomain(Domain.PERSON);
    domainValuePair.setValue("person_id");

    DataSetRequest request = new DataSetRequest();
    request.setName("New Name");
    request.setCohortIds(cohortIds);
    request.setConceptSetIds(conceptIds);
    request.setIncludesAllParticipants(false);
    request.setPrePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE));
    request.setDomainValuePairs(ImmutableList.of(domainValuePair));
    final DbDataset toDataSet = dataSetMapper.dataSetRequestToDb(request, dbDataset, fakeClock);
    assertThat(toDataSet.getName()).isEqualTo("New Name");
    assertThat(toDataSet.getCohortIds()).isEqualTo(cohortIds);
    assertThat(toDataSet.getIncludesAllParticipants()).isFalse();
    assertThat(toDataSet.getConceptSetIds()).isEqualTo(conceptIds);
    assertThat(toDataSet.getPrePackagedConceptSet().size()).isEqualTo(1);
    assertThat(toDataSet.getPrePackagedConceptSet().get(0)).isEqualTo((short) 0);
    assertThat(toDataSet.getValues().get(0).getDomainEnum()).isEqualTo(Domain.PERSON);
    assertThat(toDataSet.getInvalid()).isFalse();
  }

  private void assertDbModelToClient(DataSet dataSet, DbDataset dbDataset) {
    assertThat(dbDataset.getCohortIds())
        .isEqualTo(dataSet.getCohorts().stream().map(Cohort::getId).collect(Collectors.toList()));
    assertThat(dbDataset.getConceptSetIds())
        .isEqualTo(
            dataSet.getConceptSets().stream().map(ConceptSet::getId).collect(Collectors.toList()));
    assertThat(dbDataset.getValues())
        .isEqualTo(
            dataSet.getDomainValuePairs().stream()
                .map(
                    dvp ->
                        new DbDatasetValue(
                            DbStorageEnums.domainToStorage(dvp.getDomain()).toString(),
                            dvp.getValue()))
                .collect(Collectors.toList()));
    assertDbModelToClientLight(dataSet, dbDataset);
  }

  private void assertDbModelToClientLight(DataSet dataSet, DbDataset dbDataset) {
    assertThat(dbDataset.getDataSetId()).isEqualTo(dataSet.getId());
    assertThat(dbDataset.getVersion()).isEqualTo(Etags.toVersion(dataSet.getEtag()));
    assertThat(dbDataset.getName()).isEqualTo(dataSet.getName());
    assertThat(dbDataset.getIncludesAllParticipants())
        .isEqualTo(dataSet.getIncludesAllParticipants());
    assertThat(dbDataset.getDescription()).isEqualTo(dataSet.getDescription());
    assertThat(dbDataset.getWorkspaceId()).isEqualTo(dataSet.getWorkspaceId());
    assertThat(dbDataset.getLastModifiedTime().toInstant().toEpochMilli())
        .isEqualTo(dataSet.getLastModifiedTime());
    assertThat(dbDataset.getPrePackagedConceptSet())
        .isEqualTo(
            dataSet.getPrePackagedConceptSet().stream()
                .map(DbStorageEnums::prePackagedConceptSetsToStorage)
                .collect(Collectors.toList()));
  }

  private void assertDbModelToClient(
      DataDictionaryEntry dataDictionaryEntry, DbDSDataDictionary dbDataDictionaryEntry) {
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
  }
}
