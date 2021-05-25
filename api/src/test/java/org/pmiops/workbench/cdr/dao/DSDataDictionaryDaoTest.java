package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DSDataDictionaryDaoTest extends SpringTest {

  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;

  private DbDSDataDictionary dbDSDataDictionaryCondition_ds;
  private DbDSDataDictionary dbDSDataDictionaryCondition;
  private DbDSDataDictionary dbDSDataDictionaryPerson;

  @BeforeEach
  public void setUp() {
    dbDSDataDictionaryCondition_ds =
        dsDataDictionaryDao.save(
            DbDSDataDictionary.builder()
                .addDataProvenance("Mock Data Provenance")
                .addDescription("Mock description for Condition")
                .addDomain(Domain.CONDITION.toString())
                .addFieldName("Mock Condition Field")
                .addFieldType("Integer")
                .addOmopCdmStandardOrCustomField("Custom")
                .addRelevantOmopTable("ds_condition")
                .addSourcePpiModule("ppi")
                .build());
    dbDSDataDictionaryPerson =
        dsDataDictionaryDao.save(
            DbDSDataDictionary.builder()
                .addDataProvenance("Mock Data Provenance")
                .addDescription("Mock description for Person")
                .addDomain(Domain.PERSON.toString())
                .addFieldName("Mock Person Field")
                .addFieldType("Integer")
                .addOmopCdmStandardOrCustomField("Custom")
                .addRelevantOmopTable("ds_person")
                .addSourcePpiModule("ppi")
                .build());
    dbDSDataDictionaryCondition =
        dsDataDictionaryDao.save(
            DbDSDataDictionary.builder()
                .addDataProvenance("Mock Data Provenance")
                .addDescription("Mock description for Condition")
                .addDomain(Domain.CONDITION.toString())
                .addFieldName("Mock Condition Field")
                .addFieldType("Integer")
                .addOmopCdmStandardOrCustomField("Custom")
                .addRelevantOmopTable("condition")
                .addSourcePpiModule("ppi")
                .build());
  }

  @Test
  public void findByFieldNameAndDomain_twoEntries() {
    DbDSDataDictionary mockConditionDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(
            "Mock Condition Field", Domain.CONDITION.toString());
    assertThat(mockConditionDictionary).isNotNull();
    assertThat(mockConditionDictionary).isEqualTo(dbDSDataDictionaryCondition);
  }

  @Test
  public void findByFieldNameAndDomain_oneEntry() {
    DbDSDataDictionary mockPersonDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(
            "Mock Person Field", Domain.PERSON.toString());
    assertThat(mockPersonDictionary).isNotNull();
    assertThat(mockPersonDictionary).isEqualTo(dbDSDataDictionaryPerson);
  }

  @Test
  public void findByFieldNameAndDomainDoesNotExist() {
    DbDSDataDictionary mockNotFoundDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(
            "Does not exist", Domain.CONDITION.toString());
    assertThat(mockNotFoundDictionary).isNull();
  }
}
