package org.pmiops.workbench.dataset;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
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
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DataSetMapperTest {

  private DbDataset dbDataset;
  private DbDataDictionaryEntry dbDataDictionaryEntry;

  @Autowired private DataSetMapper dataSetMapper;
  @Autowired private ConceptSetDao mockConceptSetDao;
  @Autowired private CohortDao mockCohortDao;

  @TestConfiguration
  @Import({
    DataSetMapperImpl.class,
    CommonMappers.class,
    ConceptSetService.class,
    ConceptSetMapperImpl.class,
    CohortService.class,
    CohortMapperImpl.class
  })
  @MockBean({
    ConceptSetDao.class,
    ConceptBigQueryService.class,
    ConceptService.class,
    CohortDao.class,
    UserDao.class
  })
  static class Configuration {}

  @Before
  public void setUp() {
    dbDataset = new DbDataset();
    dbDataset.setVersion(1);
    dbDataset.setDataSetId(101L);
    dbDataset.setName("All Blue-eyed Blondes");
    dbDataset.setIncludesAllParticipants(false);
    dbDataset.setDescription("All Blue-eyed Blondes");
    dbDataset.setLastModifiedTime(Timestamp.from(Instant.now()));
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
        dataSetMapper.dbModelToClient(dbDataDictionaryEntry);
    assertDbModelToClient(toClientDataDictionaryEntry, dbDataDictionaryEntry);
  }

  @Test
  public void testDataSetRequestToDb() {
    DataSetRequest request = new DataSetRequest();
    request.setName("New Name");
    request.setPrePackagedConceptSet(PrePackagedConceptSetEnum.SURVEY);
    final DbDataset toDataSet = dataSetMapper.dataSetRequestToDb(request, dbDataset);
    assertThat(toDataSet.getName()).isEqualTo("New Name");
    assertThat(toDataSet.getCohortIds()).isEqualTo(dbDataset.getCohortIds());
    assertThat(toDataSet.getIncludesAllParticipants())
        .isEqualTo(dbDataset.getIncludesAllParticipants());
    assertThat(toDataSet.getConceptSetIds()).isEqualTo(dbDataset.getConceptSetIds());
    assertThat(toDataSet.getValues()).isEqualTo(dbDataset.getValues());
    assertThat(toDataSet.getPrePackagedConceptSet()).isEqualTo((short) 2);
  }

  @Test
  public void testDataSetRequestToDbWithNullSourceDB() {
    List<Long> conceptIds = ImmutableList.of(4l, 5l, 6l);
    List<Long> cohortIds = ImmutableList.of(1l, 2l, 3l);

    DataSetRequest request = new DataSetRequest();
    request.setName("New Name");
    request.setCohortIds(cohortIds);
    request.setConceptSetIds(conceptIds);
    request.setIncludesAllParticipants(false);
    request.setPrePackagedConceptSet(PrePackagedConceptSetEnum.NONE);
    final DbDataset toDataSet = dataSetMapper.dataSetRequestToDb(request, null);
    assertThat(toDataSet.getName()).isEqualTo("New Name");
    assertThat(toDataSet.getCohortIds()).isEqualTo(cohortIds);
    assertThat(toDataSet.getIncludesAllParticipants()).isFalse();
    assertThat(toDataSet.getConceptSetIds()).isEqualTo(cohortIds);
    assertThat(toDataSet.getConceptSetIds()).isEqualTo(conceptIds);
    assertThat(toDataSet.getPrePackagedConceptSet()).isEqualTo((short) 0);
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
            DbStorageEnums.prePackagedConceptSetsToStorage(dataSet.getPrePackagedConceptSet()));
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
