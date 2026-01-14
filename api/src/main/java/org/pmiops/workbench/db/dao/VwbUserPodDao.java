package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface VwbUserPodDao extends CrudRepository<DbVwbUserPod, Long> {
  @Query("SELECT vp FROM DbVwbUserPod vp WHERE vp.user.userId = :userId")
  DbVwbUserPod findByUserId(@Param("userId") Long userId);
}
