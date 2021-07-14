package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CriteriaMenuDao extends CrudRepository<DbCriteriaMenu, Long> {

  @Query(
      value =
          "select * from cb_criteria_menu where parent_id = :parentId and version = (select max(version) from cb_criteria_menu) order by sort_order asc",
      nativeQuery = true)
  List<DbCriteriaMenu> findByParentIdOrderBySortOrderAsc(@Param("parentId") long parentId);

  @Query(
      value =
          "select * from cb_criteria_menu where parent_id = :parentId and version = (select max(version) from cb_criteria_menu) order by sort_order asc",
      nativeQuery = true)
  List<DbCriteriaMenu> findCriteriaMenuCurrentVersion(@Param("parentId") long parentId);

  @Query(
      value =
          "select * from cb_criteria_menu where parent_id = :parentId and version = (select distinct version from cb_criteria_menu order by version desc limit 1, 1) order by sort_order asc",
      nativeQuery = true)
  List<DbCriteriaMenu> findCriteriaMenuPreviousVersion(@Param("parentId") long parentId);
}
