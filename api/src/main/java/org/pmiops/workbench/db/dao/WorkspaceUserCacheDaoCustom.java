package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbWorkspaceUserCache;

public interface WorkspaceUserCacheDaoCustom {
  int upsertAll(Iterable<DbWorkspaceUserCache> entries);
}
