package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBCriteriaAttributeDaoTest extends SpringTest {

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  private DbCriteriaAttribute attribute;

  @BeforeEach
  public void onSetup() {
    attribute =
        cbCriteriaAttributeDao.save(
            DbCriteriaAttribute.builder()
                .addConceptId(1L)
                .addConceptName("test")
                .addEstCount("10")
                .addType("type")
                .addValueAsConceptId(12345678L)
                .build());
  }

  @Test
  public void findCriteriaAttributeByConceptId() {
    List<DbCriteriaAttribute> attributes =
        cbCriteriaAttributeDao.findCriteriaAttributeByConceptId(1L);
    assertThat(attributes.size()).isEqualTo(1);
    assertThat(attributes.get(0)).isEqualTo(attribute);
  }
}
