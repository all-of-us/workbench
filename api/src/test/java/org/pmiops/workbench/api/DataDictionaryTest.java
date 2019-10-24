package org.pmiops.workbench.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.dataset.DataSetMapper;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.model.CdrVersionEntity;
import org.pmiops.workbench.db.model.DataDictionaryEntry;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class DataDictionaryTest {

  @Autowired BigQueryService bigQueryService;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired ConceptDao conceptDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired DataDictionaryEntryDao dataDictionaryEntryDao;
  @Autowired DataSetDao dataSetDao;
  @Autowired DataSetMapper dataSetMapper;
  @Autowired DataSetService dataSetService;
  @Autowired FireCloudService fireCloudService;
  @Autowired NotebooksService notebooksService;
  @Autowired WorkspaceService workspaceService;

  @Autowired DataSetController dataSetController;

  @Rule public ExpectedException expectedEx = ExpectedException.none();

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @TestConfiguration
  @Import({DataSetController.class, DataSetMapperImpl.class})
  @MockBean({
    BigQueryService.class,
    CohortDao.class,
    ConceptDao.class,
    ConceptSetDao.class,
    DataSetDao.class,
    DataSetService.class,
    FireCloudService.class,
    NotebooksService.class,
    WorkspaceService.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    CdrVersionEntity cdrVersionEntity = new CdrVersionEntity();
    cdrVersionDao.save(cdrVersionEntity);

    DataDictionaryEntry dataDictionaryEntry = new DataDictionaryEntry();
    dataDictionaryEntry.setCdrVersionEntity(cdrVersionEntity);
    dataDictionaryEntry.setDefinedTime(new Timestamp(CLOCK.millis()));
    dataDictionaryEntry.setRelevantOmopTable(ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(Domain.DRUG));
    dataDictionaryEntry.setFieldName("TEST FIELD");
    dataDictionaryEntry.setOmopCdmStandardOrCustomField("A");
    dataDictionaryEntry.setDescription("B");
    dataDictionaryEntry.setFieldType("C");
    dataDictionaryEntry.setDataProvenance("D");
    dataDictionaryEntry.setSourcePpiModule("E");
    dataDictionaryEntry.setTransformedByRegisteredTierPrivacyMethods(true);

    dataDictionaryEntryDao.save(dataDictionaryEntry);
  }

  @Test
  public void testGetDataDictionaryEntry() {
    final Domain domain = Domain.DRUG;
    final String domainValue = "FIELD NAME / DOMAIN VALUE";

    CdrVersionEntity cdrVersionEntity = new CdrVersionEntity();
    cdrVersionDao.save(cdrVersionEntity);

    DataDictionaryEntry dataDictionaryEntry = new DataDictionaryEntry();
    dataDictionaryEntry.setCdrVersionEntity(cdrVersionEntity);
    dataDictionaryEntry.setDefinedTime(new Timestamp(CLOCK.millis()));
    dataDictionaryEntry.setRelevantOmopTable(ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(domain));
    dataDictionaryEntry.setFieldName(domainValue);
    dataDictionaryEntry.setOmopCdmStandardOrCustomField("A");
    dataDictionaryEntry.setDescription("B");
    dataDictionaryEntry.setFieldType("C");
    dataDictionaryEntry.setDataProvenance("D");
    dataDictionaryEntry.setSourcePpiModule("E");
    dataDictionaryEntry.setTransformedByRegisteredTierPrivacyMethods(true);

    dataDictionaryEntryDao.save(dataDictionaryEntry);

    org.pmiops.workbench.model.DataDictionaryEntry response =
        dataSetController
            .getDataDictionaryEntry(cdrVersionEntity.getCdrVersionId(), domain.toString(), domainValue)
            .getBody();

    assertThat(response.getCdrVersionId().longValue())
        .isEqualTo(dataDictionaryEntry.getCdrVersion().getCdrVersionId());
    assertThat(new Timestamp(response.getDefinedTime()))
        .isEqualTo(dataDictionaryEntry.getDefinedTime());
    assertThat(response.getRelevantOmopTable())
        .isEqualTo(dataDictionaryEntry.getRelevantOmopTable());
    assertThat(response.getFieldName()).isEqualTo(dataDictionaryEntry.getFieldName());
    assertThat(response.getOmopCdmStandardOrCustomField())
        .isEqualTo(dataDictionaryEntry.getOmopCdmStandardOrCustomField());
    assertThat(response.getDescription()).isEqualTo(dataDictionaryEntry.getDescription());
    assertThat(response.getFieldType()).isEqualTo(dataDictionaryEntry.getFieldType());
    assertThat(response.getDataProvenance()).isEqualTo(dataDictionaryEntry.getDataProvenance());
    assertThat(response.getSourcePpiModule()).isEqualTo(dataDictionaryEntry.getSourcePpiModule());
    assertThat(response.getTransformedByRegisteredTierPrivacyMethods())
        .isEqualTo(dataDictionaryEntry.getTransformedByRegisteredTierPrivacyMethods());
  }

  @Test
  public void testGetDataDictionaryEntry_invalidCdr() {
    expectedEx.expect(BadRequestException.class);
    expectedEx.expectMessage("Invalid CDR Version");

    dataSetController.getDataDictionaryEntry(-1L, Domain.DRUG.toString(), "TEST FIELD");
  }

  @Test
  public void testGetDataDictionaryEntry_invalidDomain() {
    expectedEx.expect(BadRequestException.class);
    expectedEx.expectMessage("Invalid Domain");

    dataSetController.getDataDictionaryEntry(
        cdrVersionDao.findAll().iterator().next().getCdrVersionId(), "random", "TEST FIELD");
  }

  @Test
  public void testGetDataDictionaryEntry_notFound() {
    expectedEx.expect(NotFoundException.class);

    dataSetController.getDataDictionaryEntry(1L, Domain.DRUG.toString(), "random");
  }
}
