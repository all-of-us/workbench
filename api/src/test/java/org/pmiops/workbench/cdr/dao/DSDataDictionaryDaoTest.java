package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DSDataDictionaryDaoTest {

  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;

  private DbDSDataDictionary dbDSDataDictionary_Condition;

  @Before
  public void setUp() {
    dbDSDataDictionary_Condition =
        dsDataDictionaryDao.save(
            DbDSDataDictionary.builder()
                .addDataProvenance("Mock Data Provenance")
                .addDescription("Mock description for Condition")
                .addDomain(Domain.CONDITION.toString())
                .addFieldName("Mock Condition Field")
                .addFieldType("Integer")
                .addOmopCdmStandardOrCustomField("Custom")
                .addRelevantOmopTable("omop")
                .addSourcePpiModule("ppi")
                .build());
    dsDataDictionaryDao.save(
        DbDSDataDictionary.builder()
            .addDataProvenance("Mock Data Provenance")
            .addDescription("Mock description for Condition")
            .addDomain(Domain.CONDITION.toString())
            .addFieldName("Mock Condition Field")
            .addFieldType("Integer")
            .addOmopCdmStandardOrCustomField("Custom")
            .addRelevantOmopTable("omop_2")
            .addSourcePpiModule("ppi")
            .build());
  }

  @Test
  public void findByFieldNameAndDomain() {
    DbDSDataDictionary mockConditionDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(
            "Mock Condition Field", Domain.CONDITION.toString());
    assertThat(mockConditionDictionary).isNotNull();
    assertThat(mockConditionDictionary).isEqualTo(dbDSDataDictionary_Condition);
  }

  @Test
  public void findByFieldNameAndDomainDoesNotExist() {
    DbDSDataDictionary mockNotFoundDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(
            "Does not exist", Domain.CONDITION.toString());
    assertThat(mockNotFoundDictionary).isNull();
  }
}
