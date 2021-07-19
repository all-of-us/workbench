package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.springframework.data.repository.CrudRepository;

public interface CriteriaMenuDao extends CrudRepository<DbCriteriaMenu, Long> {
  List<DbCriteriaMenu> findByParentIdOrderBySortOrderAsc(long parentId);
}
