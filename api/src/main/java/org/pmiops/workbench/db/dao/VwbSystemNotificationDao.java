package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbVwbSystemNotification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VwbSystemNotificationDao extends CrudRepository<DbVwbSystemNotification, Long> {
  // First By is needed so that JPA ever looks for the second By.
  List<DbVwbSystemNotification> findAllByOrderByVwbSystemNotificationIdDesc();
}
