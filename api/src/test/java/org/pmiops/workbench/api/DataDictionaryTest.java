package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.cdr.CdrVersionMapper;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.genomics.GenomicExtractionService;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DataDictionaryTest extends SpringTest {

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;
  @Autowired private DataSetController dataSetController;

  private static DbCdrVersion cdrVersion;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    DataSetController.class,
    DataSetServiceImpl.class,
    DataSetMapperImpl.class,
    CdrVersionService.class,
  })
  @MockBean({
    BigQueryService.class,
    CdrBigQuerySchemaConfigService.class,
    CohortService.class,
    CohortQueryBuilder.class,
    ConceptBigQueryService.class,
    ConceptSetService.class,
    ConceptSetMapper.class,
    FireCloudService.class,
    NotebooksService.class,
    WorkspaceService.class,
    WorkspaceAuthService.class,
    AccessTierService.class,
    CdrVersionMapper.class,
    GenomicExtractionService.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao);

    DbDSDataDictionary dbDSDataDictionary = new DbDSDataDictionary();
    dbDSDataDictionary.setRelevantOmopTable(BigQueryTableInfo.getTableName(Domain.DRUG));
    dbDSDataDictionary.setFieldName("TEST FIELD");
    dbDSDataDictionary.setOmopCdmStandardOrCustomField("A");
    dbDSDataDictionary.setDescription("B");
    dbDSDataDictionary.setFieldType("C");
    dbDSDataDictionary.setDataProvenance("D");
    dbDSDataDictionary.setSourcePpiModule("E");
    dbDSDataDictionary.setDomain("Drug");

    dsDataDictionaryDao.save(dbDSDataDictionary);
  }

  @Test
  public void testGetDataDictionaryEntry() {
    final Domain domain = Domain.DRUG;
    final String domainValue = "FIELD NAME / DOMAIN VALUE";

    DbDSDataDictionary dbDSDataDictionary = new DbDSDataDictionary();
    dbDSDataDictionary.setRelevantOmopTable(BigQueryTableInfo.getTableName(domain));
    dbDSDataDictionary.setFieldName(domainValue);
    dbDSDataDictionary.setOmopCdmStandardOrCustomField("A");
    dbDSDataDictionary.setDescription("B");
    dbDSDataDictionary.setFieldType("C");
    dbDSDataDictionary.setDataProvenance("D");
    dbDSDataDictionary.setSourcePpiModule("E");
    dbDSDataDictionary.setDomain("DRUG");
    dsDataDictionaryDao.save(dbDSDataDictionary);

    DataDictionaryEntry response =
        dataSetController
            .getDataDictionaryEntry(cdrVersion.getCdrVersionId(), domain.toString(), domainValue)
            .getBody();

    assertThat(response.getRelevantOmopTable())
        .isEqualTo(dbDSDataDictionary.getRelevantOmopTable());
    assertThat(response.getFieldName()).isEqualTo(dbDSDataDictionary.getFieldName());
    assertThat(response.getOmopCdmStandardOrCustomField())
        .isEqualTo(dbDSDataDictionary.getOmopCdmStandardOrCustomField());
    assertThat(response.getDescription()).isEqualTo(dbDSDataDictionary.getDescription());
    assertThat(response.getFieldType()).isEqualTo(dbDSDataDictionary.getFieldType());
    assertThat(response.getDataProvenance()).isEqualTo(dbDSDataDictionary.getDataProvenance());
    assertThat(response.getSourcePpiModule()).isEqualTo(dbDSDataDictionary.getSourcePpiModule());
  }

  @Test
  public void testGetDataDictionaryEntry_invalidCdr() {
    assertThrows(
        BadRequestException.class,
        () -> dataSetController.getDataDictionaryEntry(-1L, Domain.DRUG.toString(), "TEST FIELD"),
        "Invalid CDR Version");
  }

  @Test
  public void testGetDataDictionaryEntry_invalidDomain() {
    assertThrows(
        BadRequestException.class,
        () ->
            dataSetController.getDataDictionaryEntry(
                cdrVersionDao.findAll().iterator().next().getCdrVersionId(),
                "random",
                "TEST FIELD"));
  }

  @Test
  public void testGetDataDictionaryEntry_notFound() {
    assertThrows(
        NotFoundException.class,
        () -> dataSetController.getDataDictionaryEntry(1L, Domain.DRUG.toString(), "random"));
  }
}
