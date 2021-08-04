package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCBMenu;
import org.springframework.data.repository.CrudRepository;

public interface CBMenuDao extends CrudRepository<DbCBMenu, Long> {
  List<DbCBMenu> findByParentIdOrderBySortOrderAsc(long parentId);
}
