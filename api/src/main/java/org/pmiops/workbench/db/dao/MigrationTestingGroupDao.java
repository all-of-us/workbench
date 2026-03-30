package org.pmiops.workbench.db.dao;

import jakarta.transaction.Transactional;
import org.pmiops.workbench.db.model.DbMigrationTestingGroup;
import org.springframework.data.repository.CrudRepository;

public interface MigrationTestingGroupDao extends CrudRepository<DbMigrationTestingGroup, Long> {
  boolean existsByUserId(Long userId);

  @Transactional
  void deleteByUserId(Long userId);
}
