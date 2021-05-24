package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CriteriaMenuDaoTest extends SpringTest {

  @Autowired private CriteriaMenuDao criteriaMenuDao;
  private DbCriteriaMenu dbCriteriaMenuParent;
  private DbCriteriaMenu dbCriteriaMenuParent1;
  private DbCriteriaMenu dbCriteriaMenuChild;

  @BeforeEach
  public void setUp() {
    dbCriteriaMenuParent =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addSortOrder(2L)
                .build());
    dbCriteriaMenuParent1 =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Procedure")
                .addGroup(true)
                .addName("Procedure")
                .addSortOrder(1L)
                .build());
    dbCriteriaMenuChild =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(dbCriteriaMenuParent.getId())
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addType("type")
                .addGroup(false)
                .addName("Condition")
                .addSortOrder(1L)
                .build());
  }

  @Test
  public void findByParentIdOrderBySortOrderAscParent() {
    assertThat(criteriaMenuDao.findByParentIdOrderBySortOrderAsc(0L))
        .isEqualTo(ImmutableList.of(dbCriteriaMenuParent1, dbCriteriaMenuParent));
  }

  @Test
  public void findByParentIdOrderBySortOrderAscChild() {
    assertThat(criteriaMenuDao.findByParentIdOrderBySortOrderAsc(1L))
        .isEqualTo(ImmutableList.of(dbCriteriaMenuChild));
  }
}
