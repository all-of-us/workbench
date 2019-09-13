package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
public class DataSetServiceTest {

  @Autowired private BigQueryService bigQueryService;
  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private ConceptBigQueryService conceptBigQueryService;
  @Autowired private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private DataSetDao dataSetDao;

  private TestMockFactory testMockFactory;
  private DataSetService dataSetService;

  @TestConfiguration
  @MockBean({
      BigQueryService.class,
      CdrBigQuerySchemaConfigService.class,
      CohortDao.class,
      ConceptBigQueryService.class,
      ConceptSetDao.class,
      CohortQueryBuilder.class,
      DataSetDao.class
  })
  static class Configuration {

  }

  @Before
  public void setUp() throws Exception {
    testMockFactory = new TestMockFactory();
    dataSetService =
        new DataSetServiceImpl(
            bigQueryService,
            cdrBigQuerySchemaConfigService,
            cohortDao,
            conceptBigQueryService,
            conceptSetDao,
            cohortQueryBuilder,
            dataSetDao);

  }

  @Test
  public void test1() throws Exception {
    assertThat(true).isTrue();
  }
}
