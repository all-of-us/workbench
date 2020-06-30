package org.pmiops.workbench.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DataSetMapperTest {

  private DbDataset dbDataset;
  private DbDataDictionaryEntry dbDataDictionaryEntry;

  @Autowired private DataSetMapper dataSetMapper;

  @TestConfiguration
  @Import({DataSetMapperImpl.class, CommonMappers.class})
  static class Configuration {}

  @Before
  public void setUp() {
    long time = new Date().getTime();

    dbDataset =
        DbDataset.builder()
            .addVersion(1)
            .addDataSetId(101L)
            .addName("All Blue-eyed Blondes")
            .addIncludesAllParticipants(false)
            .addDescription("All Blue-eyed Blondes")
            .addLastModifiedTime(new Timestamp(time))
            .addWorkspaceId(1L)
            .addPrePackagedConceptSets(
                DbStorageEnums.prePackagedConceptSetsToStorage(PrePackagedConceptSetEnum.NONE))
            .build();

    dbDataDictionaryEntry = new DbDataDictionaryEntry();
    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(1L);
    dbDataDictionaryEntry.setCdrVersion(cdrVersion);
    dbDataDictionaryEntry.setDefinedTime(new Timestamp(time));
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
  public void dbModelToClient() {
    final DataDictionaryEntry toClientDataDictionaryEntry =
        dataSetMapper.dbModelToClient(dbDataDictionaryEntry);
    assertDbModelToClient(toClientDataDictionaryEntry, dbDataDictionaryEntry);
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
