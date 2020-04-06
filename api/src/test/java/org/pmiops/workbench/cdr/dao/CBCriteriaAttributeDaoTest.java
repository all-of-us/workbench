package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBCriteriaAttributeDaoTest {

  @Autowired private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  private DbCriteriaAttribute attribute;

  @Before
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
    assertEquals(1, attributes.size());
    assertEquals(attribute, attributes.get(0));
  }
}
