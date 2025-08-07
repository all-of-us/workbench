package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import java.util.Set;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;

public interface WorkspaceUserCacheService {
  List<WorkspaceDao.WorkspaceUserCacheView> findAllActiveWorkspacesNeedingCacheUpdate();

  void updateWorkspaceUserCache(
      Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId);

  void removeInactiveWorkspaces();

  Set<String> getWorkspaceUsers(Long workspaceId);
}
