package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CriteriaMenuDaoTest extends SpringTest {

  @Autowired private CriteriaMenuDao criteriaMenuDao;
  private DbCriteriaMenu version1DbCriteriaMenuParent;
  private DbCriteriaMenu version1AnotherDbCriteriaMenuParent;
  private DbCriteriaMenu version1DbCriteriaMenuChild;
  private DbCriteriaMenu version2DbCriteriaMenuParent;
  private DbCriteriaMenu version2AnotherDbCriteriaMenuParent;
  private DbCriteriaMenu version2DbCriteriaMenuChild;

  @BeforeEach
  public void setUp() {
    version1DbCriteriaMenuParent =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addSortOrder(2L)
                .addVersion(1)
                .build());
    version1AnotherDbCriteriaMenuParent =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Procedure")
                .addGroup(false)
                .addName("Procedure")
                .addSortOrder(1L)
                .addVersion(1)
                .build());
    version1DbCriteriaMenuChild =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(version1DbCriteriaMenuParent.getId())
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addType("type")
                .addGroup(false)
                .addName("Condition")
                .addSortOrder(1L)
                .addVersion(1)
                .build());
    version2DbCriteriaMenuParent =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addGroup(true)
                .addName("Condition")
                .addSortOrder(2L)
                .addVersion(2)
                .build());
    version2AnotherDbCriteriaMenuParent =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(0L)
                .addCategory("Program Data")
                .addDomainId("Procedure")
                .addGroup(false)
                .addName("Procedure")
                .addSortOrder(1L)
                .addVersion(2)
                .build());
    version2DbCriteriaMenuChild =
        criteriaMenuDao.save(
            DbCriteriaMenu.builder()
                .addParentId(version1DbCriteriaMenuParent.getId())
                .addCategory("Program Data")
                .addDomainId("Condition")
                .addType("type")
                .addGroup(false)
                .addName("Condition")
                .addSortOrder(1L)
                .addVersion(2)
                .build());
  }

  @Test
  public void findCriteriaMenuCurrentVersionParent() {
    assertThat(criteriaMenuDao.findCriteriaMenuCurrentVersion(0L))
        .isEqualTo(
            ImmutableList.of(version2AnotherDbCriteriaMenuParent, version2DbCriteriaMenuParent));
  }

  @Test
  public void findCriteriaMenuCurrentVersionChild() {
    assertThat(criteriaMenuDao.findCriteriaMenuCurrentVersion(1L))
        .isEqualTo(ImmutableList.of(version2DbCriteriaMenuChild));
  }

  @Test
  public void findCriteriaMenuPreviousVersionParent() {
    assertThat(criteriaMenuDao.findCriteriaMenuPreviousVersion(0L))
        .isEqualTo(
            ImmutableList.of(version1AnotherDbCriteriaMenuParent, version1DbCriteriaMenuParent));
  }

  @Test
  public void findCriteriaMenuPreviousVersionChild() {
    assertThat(criteriaMenuDao.findCriteriaMenuPreviousVersion(1L))
        .isEqualTo(ImmutableList.of(version1DbCriteriaMenuChild));
  }
}
