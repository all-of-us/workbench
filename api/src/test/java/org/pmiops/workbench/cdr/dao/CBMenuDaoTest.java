package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbCBMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBMenuDaoTest extends SpringTest {

  @Autowired private CBMenuDao cbMenuDao;
  private DbCBMenu dbCBMenuParent;
  private DbCBMenu dbCBMenuParent1;
  private DbCBMenu dbCBMenuChild;

  @BeforeEach
  public void setUp() {
    dbCBMenuParent =
        cbMenuDao.save(
            DbCBMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addSortOrder(2L)
                .addIsStandard(false)
                .build());
    dbCBMenuParent1 =
        cbMenuDao.save(
            DbCBMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Procedure")
                .addGroup(true)
                .addName("Procedure")
                .addIsStandard(true)
                .addSortOrder(1L)
                .build());
    dbCBMenuChild =
        cbMenuDao.save(
            DbCBMenu.builder()
                .addParentId(dbCBMenuParent.getId())
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addType("type")
                .addGroup(false)
                .addName("Condition")
                .addIsStandard(false)
                .addSortOrder(1L)
                .build());
  }

  @Test
  public void findByParentIdOrderBySortOrderAscParent() {
    assertThat(cbMenuDao.findByParentIdOrderBySortOrderAsc(0L))
        .isEqualTo(ImmutableList.of(dbCBMenuParent1, dbCBMenuParent));
  }

  @Test
  public void findByParentIdOrderBySortOrderAscChild() {
    assertThat(cbMenuDao.findByParentIdOrderBySortOrderAsc(1L))
        .isEqualTo(ImmutableList.of(dbCBMenuChild));
  }
}
