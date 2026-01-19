package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.data.repository.CrudRepository;

public interface VwbUserPodDao extends CrudRepository<DbVwbUserPod, Long> {
  DbVwbUserPod findByUserUserId(Long userId);
}
