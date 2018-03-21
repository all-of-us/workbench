package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.AdminActionHistory;
import org.springframework.data.repository.CrudRepository;

public interface AdminActionHistoryDao extends CrudRepository<AdminActionHistory, Long> {
  // queries on admin_action_history table go here
}
