package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbAdminActionHistory;
import org.springframework.data.repository.CrudRepository;

@Deprecated // use or create an auditor in org.pmiops.workbench.actionaudit.auditors instead
public interface AdminActionHistoryDao extends CrudRepository<DbAdminActionHistory, Long> {
  // queries on admin_action_history table go here
}
